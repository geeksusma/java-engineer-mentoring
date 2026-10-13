# Bounded Context

A **Bounded Context** is the central strategic pattern of Domain-Driven Design (Eric Evans, 2003): an explicit boundary inside which a model — and the words used to describe it — has **one precise meaning**.

The problem it solves: in any real business, the same word means different things to different people. Ask what a **Product** is:

- **Sales:** something with a name and a price that a customer can buy, maybe on discount.
- **Shipping:** a box with a weight, dimensions, and "fragile" or "hazardous" flags.
- **Inventory:** a quantity on a shelf in a warehouse, with a reorder threshold.
- **Accounting:** a cost price and a tax category.

Try to build one `Product` class for all of them and you get a 60-field god object that every team is afraid to touch. Bounded contexts give up on the single model: **each context has its own model, and they integrate through explicit contracts.**

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/macrodesign/boundedcontext`](../../src/test/java/macrodesign/boundedcontext) (classes under `src/main/java/macrodesign/boundedcontext`). Run them with `mvn test`.

---

## Same word, two models

```
macrodesign.boundedcontext
├── sales        ← Product, Customer, Order, Checkout
├── shipping     ← Product, Shipment, Carrier, Warehouse
├── contracts    ← OrderPlaced (published language)
└── messaging    ← EventBus (stand-in for Kafka/RabbitMQ)
```

There are **two** classes called `Product`, on purpose:

```java
package macrodesign.boundedcontext.sales;

public record Product(String sku, String name, BigDecimal price) {
}
```

```java
package macrodesign.boundedcontext.shipping;

public record Product(String sku, int weightGrams, boolean fragile) {
}
```

```java
assertEquals(List.of("sku", "name", "price"), componentsOf(macrodesign.boundedcontext.sales.Product.class));
assertEquals(List.of("sku", "weightGrams", "fragile"), componentsOf(macrodesign.boundedcontext.shipping.Product.class));
```

This isn't duplication. Each class is the right model **for its context**: Sales has no reason to know the weight, Shipping has no reason to know the price. When Sales adds "bundle discounts", Shipping doesn't even recompile.

Words can change too, not just shapes. What Sales calls an **Order**, Shipping calls a **Shipment**; Sales' **Customer** is Shipping's **recipient address**. Inside a context, use the context's words everywhere — code, tests, conversations. That's the **ubiquitous language**, and it is only ubiquitous *within its boundary*.

---

## Each context has its own rules

Sales knows about pricing and loyalty discounts:

```java
var order = checkout.placeOrder(ANA, Map.of(MONITOR_FOR_SALE, 1, PAD_FOR_SALE, 2));

assertEquals(new BigDecimal("195.30"), order.total()); // 217.00 minus 10% loyalty discount
```

Shipping knows about weights and carriers:

```java
private static Carrier carrierFor(int weightGrams, boolean fragile) {
    if (weightGrams > COURIER_LIMIT_GRAMS) {
        return Carrier.FREIGHT;
    }
    return weightGrams > POSTAL_LIMIT_GRAMS || fragile ? Carrier.COURIER : Carrier.POSTAL;
}
```

Neither knows the other exists.

---

## Integrating contexts: a published language

Contexts still need to collaborate: when Sales takes an order, Shipping must dispatch it. They do it through a **contract** that belongs to neither model:

```java
public record OrderPlaced(String orderId, String deliveryAddress, List<Line> lines) {

    public record Line(String sku, int quantity) {
    }
}
```

Plain values only — no `sales.Product`, no `shipping.Product`. DDD calls this a **published language**: a shared, documented, versioned format (in production usually JSON/Avro/Protobuf on a topic, or a REST API spec).

Sales publishes it and moves on:

```java
bus.publish(new OrderPlaced(order.id(), customer.address(), lines.entrySet().stream()
        .map(line -> new OrderPlaced.Line(line.getKey().sku(), line.getValue()))
        ...
        .toList()));
```

Shipping subscribes and **translates at its edge** into its own model, looking up weights in its own data:

```java
public void on(OrderPlaced event) {
    var weight = 0;
    var fragile = false;
    for (var line : event.lines()) {
        var product = productsBySku.get(line.sku());
        weight += product.weightGrams() * line.quantity();
        fragile |= product.fragile();
    }
    shipments.add(new Shipment(event.orderId(), event.deliveryAddress(), weight, fragile, carrierFor(weight, fragile)));
}
```

```java
checkout.placeOrder(ANA, Map.of(MONITOR_FOR_SALE, 1, PAD_FOR_SALE, 2));

assertEquals(List.of(new Shipment("SO-1", "Gran Via 1, Madrid", 6_800, true, Carrier.COURIER)),
        warehouse.shipments());
```

The SKU is the only thing both sides share: an **identifier**, not a model.

### Enforcing the boundary

```java
@Test
void should_shareOnlyThePublishedLanguage_when_contextsIntegrate() {
    classesIn(SALES).shouldNotDependOn(SHIPPING);
    classesIn(SHIPPING).shouldNotDependOn(SALES);
}

@Test
void should_dependOnNeitherContext_when_beingTheContract() {
    classesIn(CONTRACTS).shouldNotDependOn(SALES, SHIPPING);
}
```

---

## Context mapping: how contexts relate

The relationship between two contexts is as much about **teams** as about code. DDD names the common patterns:

| Pattern | What it means |
|---|---|
| **Published Language** | A well-documented shared format for integration (our `OrderPlaced`) |
| **Open Host Service** | A context offers a public, stable API/protocol for anyone to consume |
| **Customer / Supplier** | Upstream team (supplier) plans its changes with the downstream team's (customer) needs in mind |
| **Conformist** | Downstream simply adopts the upstream model as-is (no leverage to ask for changes) |
| **Anti-Corruption Layer** | Downstream translates the upstream model to protect its own — see [next lesson](../anti-corruption-layer/anti-corruption-layer.md) |
| **Shared Kernel** | Two teams share a small piece of the model and must agree on every change to it |
| **Separate Ways** | No integration at all; duplicating is cheaper than coordinating |

Drawing these relationships for your system is called a **context map**. It's often the most useful architecture diagram a company can have.

---

## Bounded contexts and the other lessons

- **Modules and services.** A bounded context is a natural candidate for a module in a [modular monolith](../monolith-vs-modular-monolith-vs-microservices/monolith-vs-modular-monolith-vs-microservices.md), or for a microservice. Cutting services along context boundaries (not along technical layers) is what keeps you out of the distributed monolith.
- **Inside a context**, use whatever internal architecture fits — [Hexagonal](../hexagonal-ports-and-adapters/hexagonal-ports-and-adapters.md) for a rule-heavy core context, plain [N layered](../n-layered/n-layered.md) for a CRUD-ish supporting one.
- **One context, one team.** Conway's law says the system mirrors the organization; bounded contexts let you design that on purpose.

---

## How to find the boundaries

- **Listen to the language.** When the same word has different definitions or rules in two conversations, there's probably a boundary between them.
- **Follow the business capabilities**, not the data: "take orders", "ship orders", "invoice customers".
- **Look at who changes what together.** Things that always change together belong to the same context.
- **Watch for "it depends on who's asking"** in a model's fields ("this status means X for sales but Y for the warehouse").
- Workshops like **Event Storming** are built to surface these boundaries with domain experts.

---

## Further reading

- ▶️ [DDD Bounded Contexts & Subdomains](https://www.youtube.com/watch?v=NvBsEnDgA4o) — Drawing Boxes (5 min video). A visual walkthrough of ambiguous language, separating contexts, context maps, the types of subdomains, and integrating between contexts.

---

## Summary

| Concept | In this example |
|---|---|
| Bounded context | `sales`, `shipping` — each with its own model and rules |
| Same word, different model | `sales.Product` (name, price) vs `shipping.Product` (weight, fragile) |
| Ubiquitous language | "Order"/"Customer" in Sales, "Shipment"/"recipient" in Shipping |
| Published language | `contracts.OrderPlaced` — plain values, owned by neither model |
| Integration | Sales publishes an event, Shipping translates it into its own model |
| What's shared | Identifiers (SKU, order id), never model classes |
| Enforcement | `ContextBoundariesTest` |
