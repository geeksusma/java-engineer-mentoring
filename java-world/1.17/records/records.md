# Records

Java 16 finalized **records** (JEP 395), so they're part of the Java 17 LTS. A record is a class whose whole purpose is to carry data: you declare its components once, and the compiler generates the constructor, the accessors, `equals()`, `hashCode()` and `toString()`.

```java
public record Money(BigDecimal amount, String currency) {
}
```

That one line replaces roughly 50 lines of hand-written (and hand-maintained) boilerplate.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java17/records`](../../../src/test/java/java17/records) (classes under `src/main/java/java17/records`). Run them with `mvn test`.

---

## What you get for free

For `record Money(BigDecimal amount, String currency)` the compiler generates:

- `private final` fields `amount` and `currency`.
- A **canonical constructor** `Money(BigDecimal amount, String currency)`.
- **Accessors** named after the components — `amount()` and `currency()`, **not** `getAmount()`.
- `equals()` and `hashCode()` based on all components — the correct contract from the [Equals vs Hashcode](../../basics/equals-hashcode/equals-hashcode.md) lesson (`PointRecord`), with zero effort.
- A readable `toString()`:

```java
assertEquals("Money[amount=10.50, currency=EUR]", Money.euros("10.5").toString());
```

What you **can't** do:

- Extend another class — records implicitly extend `java.lang.Record`. They **can** implement interfaces.
- Be extended — records are implicitly `final`.
- Declare extra instance fields — the state is the components, nothing else. Static fields and methods are fine.

---

## Compact constructors: validate and normalize

A record can still enforce invariants. The **compact constructor** has no parameter list and no field assignments; it runs before the generated assignments, so you can validate and even reassign the parameters:

```java
public Money {
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(currency, "currency");
    if (amount.signum() < 0) {
        throw new IllegalArgumentException("amount must not be negative: " + amount);
    }
    amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    currency = currency.toUpperCase(Locale.ROOT);
}
```

```java
var money = new Money(new BigDecimal("1.005"), "usd");

assertEquals(new BigDecimal("1.00"), money.amount());
assertEquals("USD", money.currency());
```

Since **every** instance goes through the canonical constructor, it's impossible to build an invalid `Money` — the record is a proper Value Object, not just a bag of fields.

---

## Immutability: "change" means copy

Record fields are `final`, so there are no setters. To "change" one you build a new instance — usually through a *wither* method:

```java
public Money withAmount(BigDecimal newAmount) {
    return new Money(newAmount, currency);
}

public Money plus(Money other) {
    ...
    return new Money(amount.add(other.amount), currency);
}
```

The original stays untouched, which makes records safe to share between threads and to use as `HashMap` keys.

---

## Gotcha: records are only *shallowly* immutable

`final` means the field can't point to a different list — it says nothing about the list itself. Without care, the caller keeps a handle on the record's internals:

```java
public record Order(String id, List<String> items) {
    public Order {
        items = List.copyOf(items);   // defensive, unmodifiable copy
    }
}
```

```java
var items = new ArrayList<>(List.of("book"));
var order = new Order("o-1", items);

items.add("pen");                             // mutate the original list...
assertEquals(List.of("book"), order.items()); // ...the order doesn't notice

assertThrows(UnsupportedOperationException.class, () -> order.items().add("pen"));
```

Remove the `List.copyOf` and both assertions fail.

---

## Local records: named tuples inside a method

Records can be declared inside a method, which is perfect for intermediate results in a stream pipeline — instead of `Map.Entry<String, Integer>` or `Object[]`:

```java
public static Optional<String> topScorer(Map<String, List<Integer>> scoresByPlayer) {
    record PlayerTotal(String player, int total) {
    }

    return scoresByPlayer.entrySet().stream()
            .map(entry -> new PlayerTotal(
                    entry.getKey(),
                    entry.getValue().stream().mapToInt(Integer::intValue).sum()))
            .max(Comparator.comparingInt(PlayerTotal::total))
            .map(PlayerTotal::player);
}
```

---

## Records are known to reflection (and to frameworks)

```java
assertTrue(Money.class.isRecord());
assertEquals(List.of("amount", "currency"),
        Arrays.stream(Money.class.getRecordComponents()).map(RecordComponent::getName).toList());
```

This is how Jackson, Spring's `@ConfigurationProperties` and friends map records without setters.

> Records make great DTOs, value objects, events, and keys. They are **not** a fit for JPA entities: an entity needs a no-args constructor, mutable state, and identity-based equality — the opposite of a record. We'll get back to that in the JPA block.

---

## Summary

| Hand-written data class | Record |
|---|---|
| Fields, constructor, getters, `equals`, `hashCode`, `toString` written by hand | Generated from the component list |
| `getAmount()` | `amount()` |
| Easy to forget a field in `equals`/`hashCode` | Always consistent with the components |
| Validation in the constructor | Validation in the compact constructor |
| Mutable unless you're careful | Fields always `final`; copy collections to be deeply immutable |
| Can extend a class | Cannot extend (implicitly `final`, extends `Record`); can implement interfaces |
