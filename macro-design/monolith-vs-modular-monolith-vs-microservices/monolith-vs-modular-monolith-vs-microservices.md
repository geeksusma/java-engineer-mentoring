# Monolith vs Modular Monolith vs Microservices vs "Microservices"

Micro design is about decisions inside a class. **Macro design** is about decisions at system level: how the code is split into parts, which part may talk to which, and what is deployed together. The first, and most expensive to get wrong, is the **deployment architecture**:

| Style | Deployable units | Boundaries inside |
|---|---|---|
| Monolith | 1 | Usually none — anything can call anything |
| Modular monolith | 1 | Strong, explicit, enforced |
| Microservices | Many | Strong — the network enforces them |
| "Microservices" (distributed monolith) | Many | Weak — every service needs every other one |

The key idea of this lesson: **modularity and distribution are two different things.** You can have one without the other, and the worst system has the cost of distribution without the benefit of modularity.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/macrodesign/modular`](../../src/test/java/macrodesign/modular) (classes under `src/main/java/macrodesign/modular`). Run them with `mvn test`.

---

## Monolith: one deployable, no rules

A monolith is one application, one build, one deployment, usually one database. That is **not** a bad thing:

- Calling another part of the system is a method call: nanoseconds, never fails halfway, works inside one transaction.
- One repo to clone, one process to debug, one thing to deploy and monitor.
- Refactoring across the whole system is an IDE shortcut.

The problem is not the single deployable — it's that nothing stops any class from using any other class. After a few years and a few teams, "orders" reads the "catalog" tables directly, "billing" calls an internal helper of "orders", and every change ripples everywhere. That's the **big ball of mud**, and it's what people really mean when they complain about "the monolith".

The [N layered](../n-layered/n-layered.md) lesson shows the typical shape of a monolith's code.

---

## Modular monolith: one deployable, real boundaries

A modular monolith keeps the single deployment but splits the code into **modules** around business capabilities (catalog, ordering, billing — see [Bounded Context](../bounded-context/bounded-context.md)), each with:

- A small **public API** other modules may use.
- **Internals** nobody else may touch — including its tables.

```
macrodesign.modular
├── catalog
│   ├── api          ← CatalogApi, ProductView, CatalogUnavailableException   (public)
│   ├── internal     ← ProductEntity, InProcessCatalog                        (private)
│   ├── server       ← CatalogEndpoint     (only needed once it becomes a service)
│   ├── client       ← RemoteCatalogClient (only needed once it becomes a service)
│   └── CatalogModule
└── ordering
    ├── OrderingModule
    └── OrderResult
```

`ProductEntity` carries the supplier and the cost price. If `ordering` could see it, someone would eventually build a feature on top of the cost price, and the catalog team could never change it again. So the catalog only publishes a `ProductView`:

```java
public record ProductView(String sku, String name, BigDecimal price) {
}

public interface CatalogApi {

    Map<String, ProductView> findProducts(Collection<String> skus);
}
```

### Enforce the boundary, or it doesn't exist

Without JPMS, Java can't stop another package from importing a `public` class from `catalog.internal`. So we enforce it with an **architecture test** that fails the build:

```java
@Test
void should_onlyUseThePublicApi_when_orderingTalksToCatalog() {
    classesIn(ORDERING).shouldNotDependOn(CATALOG + ".internal", CATALOG + ".server", CATALOG + ".client",
            CATALOG + ".CatalogModule");
}

@Test
void should_notKnowItsConsumers_when_beingTheCatalog() {
    classesIn(CATALOG).shouldNotDependOn(ORDERING);
}
```

`classesIn(...)` comes from [`support/DependencyRule`](../../src/test/java/support/DependencyRule.java), a small helper that reads the compiled classes with the Java 25 Class-File API. In a real project, use [ArchUnit](https://www.archunit.org/) or [Spring Modulith](https://spring.io/projects/spring-modulith); both do the same thing with more features.

A module boundary that only exists in a wiki page lasts until the first deadline.

---

## Microservices: the network becomes the boundary

Microservices take the modules and **deploy each one separately**, each with its own database. You gain:

- **Independent deployment** — the catalog team ships without coordinating with ordering.
- **Independent scaling** — run 20 catalog instances and 2 ordering ones.
- **Fault isolation** — a memory leak in one service doesn't take down the rest.
- **Team autonomy** — each team owns its service, its stack and its release cadence.

And you pay for it. Our ordering module talks to the catalog through `CatalogApi`, so extracting the catalog only changes the wiring:

```java
// in-process: the catalog is a package next door
var ordering = new OrderingModule(CatalogModule.withSampleProducts());

// microservice: the same API, implemented over the network
var catalogService = new CatalogEndpoint(CatalogModule.withSampleProducts());   // runs on another machine
var ordering = new OrderingModule(new RemoteCatalogClient(catalogService::get));
```

Same code, same answer (`FromModuleToServiceTest`). But the method call has turned into a network call, and network calls **fail**:

```java
var ordering = new OrderingModule(new RemoteCatalogClient(path -> {
    throw new SocketTimeoutException("read timed out");
}));

assertInstanceOf(OrderResult.RetryLater.class, ordering.placeOrder(CART));
```

`OrderResult.RetryLater` can never happen while everything runs in one process. As soon as the catalog is remote, it's a normal day. That's why `CatalogApi` was designed **as if it could be remote** from the start: it declares its failure mode (`CatalogUnavailableException`), and it's coarse-grained.

### Chatty APIs: harmless in-process, painful over the network

`findProducts` takes a batch of SKUs (Stock Keeping Units, the identifier of each product variant). Ordering asks for the whole cart in **one** call:

```java
new OrderingModule(client).placeOrder(CART);
assertEquals(1, requests.size());

// a chatty, one-product-at-a-time style multiplies the round trips
for (var sku : CART.keySet()) {
    client.findProducts(List.of(sku));
}
assertEquals(CART.size(), requests.size());
```

In-process, ten small calls or one big one cost the same. Over the network, each call adds latency (milliseconds, not nanoseconds) and one more chance to fail. Splitting a system along chatty interfaces is how you end up with the next style.

### What else the network adds

- **No shared transaction.** "Reserve stock and create the order" across two databases needs sagas, outbox tables, idempotent consumers, eventual consistency.
- **Single points of failure.** Microservices promise fault isolation, but only if the design delivers it. If every request synchronously needs one service (catalog, auth, pricing) or one shared piece of infrastructure (a shared database, the API gateway, the message broker), then when that one thing goes down, everything goes down. A monolith fails as one unit too, but at least it has a single thing to keep alive. Here there are many, and any of them can take the rest down with it. It gets worse with **cascading failures**: a *slow* catalog makes ordering's threads pile up waiting, ordering stops answering, and its own callers start timing out. Mitigations:
  - Run **several instances** of every critical service and component behind a load balancer, never just one.
  - Put **timeouts** on every remote call, and **circuit breakers** so a struggling service gets left alone instead of retried to death.
  - Use **bulkheads** (separate thread or connection pools per dependency), so one slow dependency can't use up all of a service's resources.
  - **Degrade gracefully** instead of failing completely: serve cached data, use a fallback, or answer "retry later". That's what `OrderResult.RetryLater` does in our example.
  - Prefer **asynchronous** integration (events, queues) where the business allows it, so a service that's down delays work instead of rejecting it.
- **Distributed debugging.** One request now crosses several processes: you need correlation ids, distributed tracing and centralized logs.
- **Versioned contracts.** You can't rename a field in one commit any more; consumers deploy on their own schedule.
- **Operations.** Service discovery, load balancers, retries with backoff, circuit breakers, timeouts, N pipelines, N dashboards.

---

## "Microservices": the distributed monolith

The quotes mean: *many services, but none of the benefits*. Symptoms:

- Services **share a database**, so a schema change needs a coordinated release anyway.
- Releasing feature X means deploying services A, B and C **together, in order**.
- One user request makes a **chain** of synchronous calls A → B → C → D; if any is down, everything is down.
- Services were cut along **technical layers** ("user-service", "validation-service", "db-service") instead of business capabilities.
- Every service calls every other one — the dependency graph is a hairball.

You get all the costs of the network from the previous section and keep the coupling of the big ball of mud. It's the worst quadrant:

|  | Weak boundaries | Strong boundaries |
|---|---|---|
| **One deployable** | Big ball of mud | Modular monolith |
| **Many deployables** | Distributed monolith ("Microservices") | Microservices |

You can't fix coupling by adding a network in between. If the modules aren't independent inside one process, they won't be independent across several.

---

## So which one?

- **Start with a modular monolith** unless you have a clear reason not to. It's cheap to run, and if the boundaries are enforced it keeps you ready to extract services later.
- **Extract a service** when one module has a real need the rest doesn't share: it must scale differently, deploy at a different pace, be owned by a separate team, or be isolated for reliability or compliance.
- **Extract along module boundaries that have already proven stable** — like `catalog` above, where ordering only ever used `CatalogApi`. If the architecture tests have been green for months, the seam is real.
- Microservices are mostly about **organizational scale** (many teams working independently). Three developers rarely need twelve services.

---

## Further reading

- [Architectural patterns for modular monoliths that enable fast flow](https://microservices.io/post/architecture/2024/09/09/modular-monolith-patterns-for-fast-flow.html) — Chris Richardson (microservices.io). Covers three patterns: the modular monolith, the domain module API, and the domain API build module. Together they keep a large single codebase manageable, give teams more autonomy, and speed up the deployment pipeline. Our `catalog.api` package is a small version of the domain module API.

---

## Summary

|  | Monolith | Modular monolith | Microservices | "Microservices" |
|---|---|---|---|---|
| Deployables | 1 | 1 | Many | Many |
| Calls between parts | Method calls | Method calls through a module API | Network | Network, and lots of it |
| Boundaries | Convention (if any) | Enforced by architecture tests | Enforced by the network | Bypassed via shared DB / chatty calls |
| Transactions | Local | Local | Sagas, eventual consistency | Painful sagas or none at all |
| Deploy independently | No | No | Yes | Not really |
| Operational cost | Low | Low | High | High |
| Good default? | For small/short-lived apps | **Yes** | When scale and teams demand it | Never |
