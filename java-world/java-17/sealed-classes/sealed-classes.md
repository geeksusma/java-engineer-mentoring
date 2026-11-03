# Sealed Classes

Java 17 made **sealed classes and interfaces** final (JEP 409, after previews in 15 and 16). A sealed type lists exactly which types are allowed to extend or implement it. Before this, a class hierarchy had only two options: `final` (nobody can extend it) or open (*anybody* can extend it, forever). Sealed types add the missing middle ground: "these subtypes, and no others".

```java
public sealed interface Shape permits Circle, Square, Polygon {
}
```

Any other class writing `implements Shape` gets a compile error — even if it lives in the same package.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java17/sealed`](../../../src/test/java/java17/sealed) (classes under `src/main/java/java17/sealed`). Run them with `mvn test`.

---

## Why would I want to close a hierarchy?

Because many domain concepts genuinely have a fixed set of variants: a payment is a card, a bank transfer or a voucher; a shape is a circle, a square or a polygon; an HTTP response is a success or a failure. With an open hierarchy, the compiler has to assume a new variant can appear at any time, so:

- Every `if/else instanceof` chain or `switch` needs a defensive `default` / `else throw`.
- Adding a new variant silently compiles, and the bug only shows up at runtime when that `default` branch fires.

A sealed hierarchy tells the compiler the full list, so it can check your code against it.

---

## Every permitted subtype must pick a side: `final`, `sealed` or `non-sealed`

Sealing only means something if the permitted subtypes can't quietly reopen the hierarchy. So each one must say explicitly what happens below it:

```java
// Records are implicitly final — nothing more to say.
public record Circle(double radius) implements Shape {
}

// final: the hierarchy ends here.
public final class Square implements Shape { ... }

// non-sealed: anyone may extend Polygon again.
public abstract non-sealed class Polygon implements Shape {
    public abstract double area();
}

// Not in Shape's permits list, but legal because Polygon is non-sealed.
public class Triangle extends Polygon { ... }
```

`non-sealed` is a deliberate, visible escape hatch: the *direct* subtypes of `Shape` are still exactly three, but `Polygon` is an open extension point. A `Triangle` is still a `Shape` — via `Polygon`.

The third option, `sealed`, lets a subtype declare its own `permits` list, building a closed tree several levels deep.

---

## The hierarchy is visible at runtime too

The reflection API knows about sealing:

```java
assertTrue(Shape.class.isSealed());
assertEquals(
        Set.of(Circle.class, Square.class, Polygon.class),
        Set.of(Shape.class.getPermittedSubclasses()));

assertFalse(Polygon.class.isSealed());  // non-sealed
```

---

## The real payoff: exhaustive `switch` without `default`

Combined with pattern matching for `switch` (final in Java 21, see [Pattern matching](../pattern-matching-vs-instanceof-and-switch-case/pattern-matching-vs-instanceof-and-switch-case.md)), the compiler can **prove** a switch covers every case:

```java
public static double of(Shape shape) {
    return switch (shape) {
        case Circle c -> Math.PI * c.radius() * c.radius();
        case Square s -> s.side() * s.side();
        case Polygon p -> p.area();
    };
}
```

There's no `default`. The day someone adds `Hexagon` to `permits`, this method **stops compiling** until it handles the new case. With an open hierarchy you'd need a `default -> throw new IllegalStateException()` and you'd only find out in production.

> Rule of thumb: **don't add a `default` branch to a switch over a sealed type.** It turns a compile-time error into a runtime one.

---

## Rules worth knowing

- Permitted subtypes must be in the same module (or, without modules, the same package) as the sealed type.
- If the sealed type and all its subtypes are in the same source file, the `permits` clause can be omitted — the compiler infers it (see `Event` in the pattern matching lesson).
- Sealed types pair naturally with records: a sealed interface for the "one of" and records for each variant — what other languages call *algebraic data types* or *sum types*.

---

## Summary

| Modifier on a subtype | Meaning |
|---|---|
| `final` (or a record/enum) | Nothing can extend it — the branch ends here |
| `sealed ... permits ...` | Closed again, with its own explicit list |
| `non-sealed` | Reopened: anyone can extend it from here down |

| Open hierarchy | Sealed hierarchy |
|---|---|
| Any class, anywhere, can add a variant | Only the listed variants exist |
| `switch` needs a defensive `default` | Compiler proves the `switch` is exhaustive |
| A new variant fails at runtime | A new variant fails at compile time, pointing at every place that must handle it |
