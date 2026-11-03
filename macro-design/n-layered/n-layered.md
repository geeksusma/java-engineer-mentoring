# N Layered

The **layered architecture** is the one you'll find in most enterprise Java codebases, and the one every Spring tutorial produces: split the code into horizontal layers, and let each layer only call the one below.

```
┌────────────────────────┐
│  Presentation          │  controllers, views, request/response
├────────────────────────┤
│  Business (service)    │  rules, orchestration, transactions
├────────────────────────┤
│  Persistence (DAO)     │  SQL, rows, repositories
└────────────────────────┘
            │
        Database
```

"N" is because the number varies: 3 is classic (presentation, business, persistence), and some add an *application* or *integration* layer. It's simple, everybody understands it, and it's a fine choice for CRUD-heavy apps. Its weak spot is one arrow: **the business layer depends on the persistence layer.** We'll make that visible with tests, because the next two lessons ([Hexagonal](../hexagonal-ports-and-adapters/hexagonal-ports-and-adapters.md) and [Clean Architecture](../clean-architecture/clean-architecture.md)) exist to flip it.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/macrodesign/nlayered`](../../src/test/java/macrodesign/nlayered) (classes under `src/main/java/macrodesign/nlayered`). Run them with `mvn test`.

---

## The example: placing an order

```
macrodesign.nlayered
├── presentation   ← OrderController, Response
├── business       ← OrderService, OrderRejectedException
└── persistence    ← ProductDao, OrderDao, ProductRow, OrderRow, Database
```

A request flows down through every layer:

```java
public Response post(String customerEmail, String productCode, int quantity) {      // presentation
    try {
        var order = orderService.placeOrder(customerEmail, productCode, quantity);
        return new Response(201, "order #" + order.id() + " total " + order.total());
    } catch (OrderRejectedException e) {
        return new Response(400, e.getMessage());
    }
}
```

```java
public OrderRow placeOrder(String customerEmail, String productCode, int quantity) { // business
    ...
    var product = productDao.findByCode(productCode)
            .orElseThrow(() -> new OrderRejectedException("unknown product: " + productCode));
    if (product.stock() < quantity) {
        throw new OrderRejectedException("only " + product.stock() + " left of " + productCode);
    }
    productDao.updateStock(productCode, product.stock() - quantity);
    var total = product.unitPrice().multiply(BigDecimal.valueOf(quantity));
    return orderDao.insert(customerEmail, productCode, quantity, total);
}
```

```java
assertEquals(new Response(201, "order #1 total 99.80"), controller.post("ana@example.com", "KB-01", 2));
assertEquals(new Response(400, "only 3 left of KB-01"), controller.post("ana@example.com", "KB-01", 5));
```

It works, it's readable, and a junior can find their way around it on day one. That's the real strength of layering.

---

## The rule: dependencies only go down

Layering has one rule — **a layer never depends on the layers above it** — and it can be tested:

```java
@Test
void should_onlyDependDownwards_when_layeringIsRespected() {
    classesIn(BUSINESS).shouldNotDependOn(PRESENTATION);
    classesIn(PERSISTENCE).shouldNotDependOn(BUSINESS, PRESENTATION);
}
```

`classesIn(...)` comes from [`support/DependencyRule`](../../src/test/java/support/DependencyRule.java) (explained in the [Monolith vs Microservices](../monolith-vs-modular-monolith-vs-microservices/monolith-vs-modular-monolith-vs-microservices.md#enforce-the-boundary-or-it-doesnt-exist) lesson).

There are two flavours:

- **Strict layering:** a layer may only use the layer *directly* below. The controller never touches a DAO.
- **Relaxed layering:** a layer may use *any* layer below. Less ceremony for simple reads, but the layers become less meaningful.

---

## Problem 1: the domain is the database

Look at what `OrderService` returns: an `OrderRow`, a type from the persistence layer. There is no "order" concept anywhere except the table shape. And because the row is returned up the stack, it leaks into the presentation layer too — even though `OrderController` never imports the persistence package:

```java
@Test
void should_leakPersistenceTypesToPresentation_when_rowsAreTheModel() {
    assertTrue(DependencyRule.dependenciesOf(OrderController.class)
            .contains("macrodesign.nlayered.persistence.OrderRow"));
    assertThrows(AssertionError.class, () -> classesIn(PRESENTATION).shouldNotDependOn(PERSISTENCE));
}
```

So even strict layering ends up relaxed in practice: add a column to `ORDERS`, and the change can show up in the JSON response. The layers are stacked, but they aren't **isolated**.

The usual fix is to map rows to domain objects inside the service and to DTOs inside the controller. That helps, but the domain objects are still defined by — and still depend on — whatever the persistence layer offers.

---

## Problem 2: the business layer depends on infrastructure

This is the main issue. The arrow goes from the most important code (business rules) to the most volatile code (database access):

```java
public class OrderService {

    private final ProductDao productDao;   // concrete classes from the layer below
    private final OrderDao orderDao;
    ...
}
```

```java
@Test
void should_dependOnPersistence_when_businessLayerIsClassic() {
    assertTrue(DependencyRule.dependenciesOf(OrderService.class)
            .contains("macrodesign.nlayered.persistence.ProductRow"));

    // the rule hexagonal and clean architecture enforce fails here
    assertThrows(AssertionError.class, () -> classesIn(BUSINESS).shouldNotDependOn(PERSISTENCE));
}
```

Consequences:

- **Changing the database changes the business layer.** Moving from JDBC to JPA, or from SQL to a document store, means touching the service.
- **Testing a business rule needs the database.** There's no seam to plug a fake in:

```java
@Test
void should_needARealDatabase_when_unitTestingTheBusinessLayer() {
    // OrderService takes concrete DAOs: there is no seam to plug a fake in,
    // so even a "unit" test of a business rule has to build the database.
    var service = new OrderService(new ProductDao(database), new OrderDao(database));

    service.placeOrder("ana@example.com", "KB-01", 1);

    assertEquals(3, database.queries());
}
```

  In a real app, `Database` is PostgreSQL in Testcontainers, or a pile of mocks of the DAO classes — slow tests, or tests that check implementation details.
- **The business layer is designed bottom-up.** You model the tables first and write the rules around them, instead of the other way round.

> In a Spring app, `OrderService` would take `OrderRepository extends JpaRepository<OrderEntity, Long>`. It *looks* like an interface, but it's a JPA interface over a JPA entity: the business layer still depends on the persistence technology.

---

## The fix in one sentence: invert that arrow

What if the business layer **declared the interface** it needs ("give me a product by code", "store this order"), and the persistence layer **implemented** it? Then the business layer depends on nothing, and the database depends on the business layer. That is the Dependency Inversion Principle (the **D** in SOLID) applied at system level, and it's exactly what [Hexagonal (Ports and Adapters)](../hexagonal-ports-and-adapters/hexagonal-ports-and-adapters.md) does — with the very same "place order" use case, so you can compare them line by line.

---

## When N layered is the right call

- CRUD apps where the "business logic" is mostly validation and mapping.
- Short-lived apps, prototypes, internal tools.
- Teams that need the lowest possible onboarding cost.

When rules grow (pricing, stock reservation, workflows), and you want fast tests for them, the downward arrow starts to hurt.

---

## Common pitfalls

Both pitfalls below are about **one class doing the job of several models**. Each layer has its own reason to change, so each needs its own shape:

| Model | Layer | Shaped by |
|---|---|---|
| Request/response **DTO** | Presentation | The API contract with clients |
| **Domain** object | Business | The business rules and their invariants |
| Database **entity** / row | Persistence | The schema and the ORM's needs |

### Using the database entity as the business model

The full-size version of `OrderService` returning an `OrderRow`: the JPA `@Entity` *is* the domain, and business logic is written against it.

```java
@Entity
public class Order {
    @Id @GeneratedValue private Long id;
    private String status;                                // free text, anyone can set anything
    @OneToMany(mappedBy = "order") private List<OrderLine> lines;

    protected Order() { }                                 // required by JPA
    public void setStatus(String status) { ... }          // setters for the ORM, open to everyone
}

// in the service
order.setStatus("PAID");
var total = order.getLines().stream()...                  // LazyInitializationException outside a transaction
```

What goes wrong:

- **Invariants can't be protected.** JPA wants a no-args constructor and (usually) setters, so an `Order` can exist half-built or in an impossible state. Compare with the compact constructor of a record in the [Records](../../java-world/java-17/records/records.md) lesson, where an invalid object can't even be created.
- **ORM behaviour leaks into business rules.** Lazy loading, proxies and dirty checking: reading a collection can fire a query or throw `LazyInitializationException`, and changing a field in memory can silently become an `UPDATE` at commit.
- **The schema dictates the model.** Splitting a table or renaming a column means touching business code. And the model can only hold what maps easily to columns.
- **Equality gets tricky.** Entities have identity-based equality and a generated id that is `null` until saved — see the [Equals vs Hashcode](../../java-world/basics/equals-hashcode/equals-hashcode.md) lesson for why that matters in `HashSet`s and `HashMap`s.
- **It usually reaches the API too.** Returned straight from the controller, the entity exposes internal fields, couples the JSON to the schema, and bidirectional relations cause infinite recursion when serialized.

### Persisting the incoming DTO

The opposite shortcut: what the client sent goes straight into the database, skipping the business layer.

```java
@PostMapping("/orders")
public Order create(@RequestBody Order order) {   // the @Entity doubles as the request body
    return orderRepository.save(order);
}
```

What goes wrong:

- **Mass assignment (over-posting).** The client controls every mapped field, not just the ones the form shows: `{"id": 42, "status": "PAID", "total": 0.01}` overwrites another order, marks it paid, and sets the price. This is a real security vulnerability (OWASP's "Mass Assignment").
- **The business layer is bypassed.** Stock isn't reserved, the total isn't computed by us, no rule runs. The only thing standing between the request and the database is input validation.
- **The API contract and the schema are locked together.** Renaming a column breaks clients; adding a field to the API needs a migration; versioning the API independently is impossible.
- **Input validation and domain invariants get mixed.** "The email field is required in this form" and "an order can't have zero lines" are different rules with different owners, all ending up as annotations on one class.

### The fix: one model per layer, mapping at the boundaries

```
OrderRequest (DTO) ──controller maps──▶ business call ──▶ Order (domain) ──DAO maps──▶ OrderEntity / row
OrderResponse (DTO) ◀──controller maps── Order (domain) ◀──DAO maps── OrderEntity / row
```

- The controller accepts a **request DTO** with only the fields a client may send, and returns a **response DTO** with only what clients may see. Records are a great fit for both.
- The business layer works with **domain objects** that protect their invariants.
- The persistence layer maps domain objects to and from **entities/rows**, and keeps ORM types to itself.

Yes, that's more classes and some mapping code. For a pure CRUD screen where the three shapes really are identical, sharing one class can be a conscious trade-off — but never let a client-supplied object reach the database without deciding which fields it may set. And once the shapes start to differ, split them.

---

## Summary

| Aspect | N layered |
|---|---|
| Structure | Horizontal layers: presentation → business → persistence |
| Rule | Dependencies only point down (strict: only to the next layer) |
| Strength | Simple, familiar, little ceremony |
| Domain model | Often the table shape (rows/entities travel up the stack) |
| Business depends on | The persistence layer (concrete DAOs / JPA repositories) |
| Testing business rules | Needs a database or mocks of DAO classes |
| Swapping infrastructure | Touches the business layer |
| Common pitfalls | Entity used as the domain model; incoming DTO persisted as-is — use one model per layer |
| Next step | Invert the business → persistence arrow: Hexagonal / Clean |
