# Pattern matching vs instanceof and switch case

Pattern matching lets a single expression **test** a value's shape, **cast** it, and **bind** the pieces to variables. It arrived in steps:

| Feature | Final in |
|---|---|
| Switch expressions (`->`, `yield`) | Java 14 |
| Pattern matching for `instanceof` | Java 16 — available in 17 |
| Pattern matching for `switch` (type patterns, guards, `case null`) | Java 21 (preview in 17) |
| Record patterns (deconstruction) | Java 21 |
| Primitive types in patterns | Preview in 25 — see [the 1.25 lesson](../../1.25/pattern-matching-with-primitive-types/pattern-matching-with-primitive-types.md) |

Java 17 is where most teams first met it, so it's covered here — using the Java 21+ `switch` forms since this project runs on Java 25.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java17/patternmatching`](../../../src/test/java/java17/patternmatching) (classes under `src/main/java/java17/patternmatching`). Run them with `mvn test`.

---

## The old way: test, then cast again

```java
if (value instanceof Integer) {
    Integer number = (Integer) value;   // we *just* checked this...
    if (number < 0) {
        return "negative integer " + number;
    }
    return "integer " + number;
}
if (value instanceof String) {
    String text = (String) value;
    ...
}
```

Every branch repeats the type three times (test, declaration, cast). It's noisy, and a copy-paste slip — testing `String` but casting to `Integer` — compiles fine and blows up with `ClassCastException` at runtime.

---

## Pattern matching for `instanceof`

```java
if (value instanceof String text && !text.isBlank()) {
    return text.length();
}
```

`instanceof String text` tests, casts and binds in one go. The binding `text` is only in scope where the compiler knows the match succeeded — which includes the right side of `&&`.

It even works "backwards", which is called **flow scoping**:

```java
public static int lengthOrMinusOne(Object value) {
    if (!(value instanceof String text)) {
        return -1;
    }
    return text.length();   // in scope: we only get here if the match succeeded
}
```

---

## Pattern matching for `switch`

The same `LegacyDescriber` logic becomes a single switch expression in `ModernDescriber`:

```java
public static String describe(Object value) {
    return switch (value) {
        case null -> "null";
        case Integer number when number < 0 -> "negative integer " + number;
        case Integer number -> "integer " + number;
        case String text when text.isBlank() -> "blank string";
        case String text -> "string of length " + text.length();
        case int[] numbers -> "int array of " + numbers.length;
        default -> "unknown: " + value.getClass().getSimpleName();
    };
}
```

What changed compared to the classic `switch`:

- **Any reference type** can be switched on, not only `int`, `String` and enums.
- **Type patterns** (`case Integer number`) replace `instanceof` + cast.
- **Guards** (`when number < 0`) replace nested `if`s.
- **`case null`** is allowed. Without it, `switch` still throws `NullPointerException` on `null`, as it always did.
- **Arrow form** (`->`) has no fall-through, so no forgotten `break`.
- It's an **expression**: it returns a value, and the compiler checks every path does.

A parameterized test runs both describers against the same inputs and asserts identical output — same behaviour, a fraction of the ceremony.

### Order matters

Cases are tried top to bottom. A more specific case must come *before* a more general one, otherwise the compiler rejects it as **dominated**:

```java
case Integer number -> ...
case Integer number when number < 0 -> ...   // compile error: dominated by the case above
```

---

## Record patterns: deconstruct while matching

Combine a sealed interface with records and the switch can pull the components straight out:

```java
public sealed interface Event {
    record Login(String user) implements Event {}
    record Logout(String user) implements Event {}
    record Purchase(String user, BigDecimal amount) implements Event {}
}
```

```java
public static String format(Event event) {
    return switch (event) {
        case Login(var user) -> user + " logged in";
        case Logout(var user) -> user + " logged out";
        case Purchase(var user, var amount) when amount.compareTo(LARGE_PURCHASE) >= 0 ->
                "LARGE purchase by " + user + ": " + amount;
        case Purchase(var user, var amount) -> user + " bought for " + amount;
    };
}
```

- `Purchase(var user, var amount)` matches a `Purchase` **and** binds its two components — no `purchase.user()` calls.
- Because `Event` is sealed, the switch is **exhaustive without `default`** (see [Sealed Classes](../sealed-classes/sealed-classes.md)).
- The `permits` clause is missing on purpose: when all subtypes are in the same file, the compiler infers it.

---

## Summary

| Before | With pattern matching |
|---|---|
| `if (o instanceof String) { String s = (String) o; ... }` | `if (o instanceof String s) { ... }` |
| `switch` only on `int`, `String`, enums | `switch` on any type, by type pattern |
| Nested `if` inside each `case` | `case X x when condition` guards |
| `switch` throws NPE on `null` | `case null ->` handles it explicitly |
| Fall-through and forgotten `break` | Arrow cases never fall through |
| Getter calls after the cast | Record patterns bind components directly |
| `default -> throw` "just in case" | Exhaustive switch over sealed types, checked at compile time |
