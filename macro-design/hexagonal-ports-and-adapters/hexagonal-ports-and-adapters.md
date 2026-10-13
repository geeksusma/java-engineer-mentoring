# Hexagonal (Ports and Adapters)

Alistair Cockburn's **Hexagonal Architecture** (2005), also called **Ports and Adapters**, has one goal: *let the application be driven equally by users, programs, tests or scripts, and be developed and tested in isolation from its databases and devices.*

The trick is to stop thinking in layers (top/bottom) and think in **inside/outside**:

```
  driving adapters               the hexagon (core)                    driven adapters
  (use the app)                                                        (used by the app)

                       ┌──────────────────────────────────────┐
  OrderController  ───▶│ PlaceOrderUseCase          (in port) │
  OrderCommandLine ───▶│                                      │
                       │ PlaceOrderService                    │
                       │ Product, Order, ProductCode (domain) │
                       │                                      │
                       │ ProductCatalog           (out ports) │◀─── InMemoryProductCatalog
                       │ OrderRepository                      │◀─── InMemoryOrderRepository
                       │ OrderConfirmationSender              │◀─── OutboxConfirmationSender
                       └──────────────────────────────────────┘

                every arrow is a source-code dependency: they all point inwards
```

- The **core** (domain + application) holds the business logic and knows nothing about HTTP, SQL, Kafka or Spring.
- A **port** is an interface owned by the core, written in the core's language.
- An **adapter** connects one technology to one port. Adapters live outside and depend on the core — **never the other way round**.

The hexagon shape means nothing — it just leaves room to draw many ports.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/macrodesign/hexagonal`](../../src/test/java/macrodesign/hexagonal) (classes under `src/main/java/macrodesign/hexagonal`). Run them with `mvn test`. It's the same "place order" use case as the [N layered](../n-layered/n-layered.md) lesson, so compare them side by side.

---

## The package layout

```
macrodesign.hexagonal
├── domain                         ← Product, Order, ProductCode, OrderId, OrderRejectedException
├── application
│   ├── port
│   │   ├── in                     ← PlaceOrderUseCase, PlaceOrderCommand        (driving ports)
│   │   └── out                    ← ProductCatalog, OrderRepository,           (driven ports)
│   │                                OrderConfirmationSender
│   └── PlaceOrderService          ← implements the driving port, uses the driven ones
├── adapter
│   ├── in
│   │   ├── web                    ← OrderController, Response
│   │   └── cli                    ← OrderCommandLine
│   └── out
│       ├── persistence            ← InMemoryProductCatalog, InMemoryOrderRepository
│       └── notification           ← OutboxConfirmationSender
└── HexagonalApp                   ← composition root: plugs adapters into ports
```

---

## Driving ports: how the world uses the application

A **driving** (primary) port says what the application *offers*:

```java
public interface PlaceOrderUseCase {

    Order placeOrder(PlaceOrderCommand command);
}
```

A **driving adapter** turns a technology into a call on that port. The web adapter knows HTTP, the CLI adapter knows command lines — both only know `PlaceOrderUseCase`, never `PlaceOrderService`:

```java
public Response post(String customerEmail, String productCode, int quantity) {
    try {
        var order = placeOrder.placeOrder(new PlaceOrderCommand(customerEmail, new ProductCode(productCode), quantity));
        return new Response(201, "order #" + order.id().value() + " total " + order.total());
    } catch (OrderRejectedException e) {
        return new Response(400, e.getMessage());
    }
}
```

```java
assertEquals(new Response(201, "order #1 total 49.90"), app.web.post("ana@example.com", "KB-01", 1));
assertEquals("OK 2", app.cli.run("order bob@example.com KB-01 1"));
```

Adding a Kafka consumer or a scheduled job tomorrow is one more adapter. The core doesn't change.

---

## Driven ports: what the application needs from the world

A **driven** (secondary) port says what the application *needs*, in its own terms:

```java
public interface ProductCatalog {

    Optional<Product> findByCode(ProductCode code);

    void save(Product product);
}
```

This is the key difference from the [N layered](../n-layered/n-layered.md) version. There, `OrderService` depended on `ProductDao` and its `ProductRow`. Here the **core owns the interface**, and the database adapter implements it:

```
N layered:   OrderService ──────────▶ ProductDao (persistence)
Hexagonal:   PlaceOrderService ─────▶ ProductCatalog (core)  ◀────── InMemoryProductCatalog (adapter)
```

The arrow from the business code to the database code has been **inverted** — that's the Dependency Inversion Principle at architecture scale. A JPA adapter would implement the same port, map its `@Entity` to `Product`, and keep JPA types inside its own package.

---

## The core: rules, not plumbing

The rules live in the domain, and the service only orchestrates:

```java
public record Product(ProductCode code, BigDecimal unitPrice, int stock) {

    public Product reserve(int quantity) {
        if (quantity <= 0) {
            throw new OrderRejectedException("quantity must be positive: " + quantity);
        }
        if (stock < quantity) {
            throw new OrderRejectedException("only " + stock + " left of " + code.value());
        }
        return new Product(code, unitPrice, stock - quantity);
    }
    ...
}
```

```java
@Override
public Order placeOrder(PlaceOrderCommand command) {
    var product = catalog.findByCode(command.productCode())
            .orElseThrow(() -> new OrderRejectedException("unknown product: " + command.productCode().value()));

    catalog.save(product.reserve(command.quantity()));
    var order = new Order(orders.nextId(), command.customerEmail(), product.code(), command.quantity(),
            product.priceFor(command.quantity()));
    orders.save(order);
    confirmations.sendConfirmation(order);
    return order;
}
```

---

## The payoff: testing the core in isolation

Because every dependency is a port the core owns, the test can provide tiny fakes — a lambda is enough for a one-method port:

```java
private final List<Order> confirmed = new ArrayList<>();
private final OrderConfirmationSender confirmations = confirmed::add;

private final PlaceOrderService service = new PlaceOrderService(catalog, orders, confirmations);

@Test
void should_touchNoOutputPort_when_stockIsInsufficient() {
    catalog.save(new Product(KEYBOARD, new BigDecimal("49.90"), 1));

    assertThrows(OrderRejectedException.class,
            () -> service.placeOrder(new PlaceOrderCommand("ana@example.com", KEYBOARD, 2)));

    assertEquals(1, catalog.findByCode(KEYBOARD).orElseThrow().stock());
    assertTrue(savedOrders.isEmpty());
    assertTrue(confirmed.isEmpty());
}
```

No database, no Testcontainers, no Mockito, milliseconds per test. Compare with the N layered `should_needARealDatabase_when_unitTestingTheBusinessLayer`.

Adapters get their own, narrower tests: a JPA adapter tested against a real database, a REST adapter tested for status codes. Each test checks one thing.

---

## The composition root

Someone has to know every concrete class and plug adapters into ports. That code lives **outside** the hexagon:

```java
public final class HexagonalApp {

    public final InMemoryProductCatalog catalog = new InMemoryProductCatalog();
    public final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    public final OutboxConfirmationSender confirmations = new OutboxConfirmationSender();

    private final PlaceOrderService placeOrder = new PlaceOrderService(catalog, orders, confirmations);

    public final OrderController web = new OrderController(placeOrder);
    public final OrderCommandLine cli = new OrderCommandLine(placeOrder);
}
```

In Spring, the container is the composition root: `@Configuration` classes or component scanning do this wiring. Keep Spring annotations in the adapters and configuration, and the core stays plain Java.

---

## Enforcing it

Like any architecture, it only holds if the build fails when someone breaks it:

```java
@Test
void should_dependOnNothingOutside_when_inTheDomain() {
    classesIn(DOMAIN).shouldNotDependOn(APPLICATION, ADAPTERS);
}

@Test
void should_neverSeeAnAdapter_when_inTheApplicationCore() {
    classesIn(APPLICATION).shouldNotDependOn(ADAPTERS);
}

@Test
void should_onlyMeetThroughPorts_when_adaptersAreOnOppositeSides() {
    classesIn(DRIVING_ADAPTERS).shouldNotDependOn(DRIVEN_ADAPTERS, "macrodesign.hexagonal.application.PlaceOrderService");
    classesIn(DRIVEN_ADAPTERS).shouldNotDependOn(DRIVING_ADAPTERS);
}
```

Try it: make `PlaceOrderService` take an `InMemoryProductCatalog` instead of a `ProductCatalog`, and `HexagonalArchitectureTest` goes red with the exact offending reference.

---

## Common mistakes

- **Ports shaped like the technology.** `ProductCatalog.executeQuery(String sql)` is not a port, it's a leak. Ports speak the domain: `findByCode(ProductCode)`.
- **A domain coupled to frameworks or libraries.** The domain must be as agnostic as possible: plain Java, the JDK, and nothing else. Every framework or library it imports becomes part of your business rules, and you inherit its upgrades, breaking changes, security advisories and opinions about how objects should look. Typical leaks:
  - `@Entity`, `@Column` (JPA) or `@JsonProperty` (Jackson) on domain objects — now persistence and serialization formats shape the model. Map in the adapter instead.
  - `@Service`, `@Transactional`, `@Autowired` (Spring) in the core — the domain can't be built or tested without the container. Wire it in the composition root, and put transactions on the driving adapter or a thin application-level wrapper.
  - Vendor SDK types (an AWS, Stripe or Kafka client class) in a port signature — the port now speaks the vendor's language. That belongs in a driven adapter, or an [Anti-Corruption Layer](../anti-corruption-layer/anti-corruption-layer.md) when the vendor model is messy.

  A small, stable utility library may be an acceptable trade-off, but it should be a deliberate team decision, not something that slips in with an import. The architecture test fails the build if a framework sneaks in:

  ```java
  classesIn(DOMAIN, APPLICATION).shouldNotDependOn(
          "org.springframework", "jakarta", "javax.persistence", "com.fasterxml.jackson", "org.hibernate");
  ```
- **One port per table.** Ports are defined by what the use case needs, not by the database schema.
- **Interfaces for everything.** Domain classes like `Product` don't need an interface; ports exist only at the boundary with the outside world.
- **Too much ceremony for CRUD.** If a use case is "save what the form sent", a port, a command, a service and two mappers may be more code than value.

---

## Summary

| Concept | In this example |
|---|---|
| Core (inside) | `domain` + `application` — plain Java, no frameworks |
| Driving port | `PlaceOrderUseCase` — what the app offers |
| Driving adapter | `OrderController`, `OrderCommandLine` — HTTP / CLI → port |
| Driven port | `ProductCatalog`, `OrderRepository`, `OrderConfirmationSender` — what the app needs |
| Driven adapter | `InMemoryProductCatalog`, `InMemoryOrderRepository`, `OutboxConfirmationSender` |
| Composition root | `HexagonalApp` (in Spring: the container) |
| Key rule | Adapters depend on the core; the core depends on nothing |
| Main payoff | Core tested without infrastructure; technology swappable per adapter |
