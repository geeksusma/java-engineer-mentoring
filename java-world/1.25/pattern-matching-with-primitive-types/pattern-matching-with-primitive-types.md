# Pattern matching with primitive types in switch and instanceof

The [Java 17 pattern matching lesson](../../1.17/pattern-matching-vs-instanceof-and-switch-case/pattern-matching-vs-instanceof-and-switch-case.md) showed type patterns, guards and record patterns — all limited to **reference types**. Java 25 continues extending them to **primitives** (JEP 507, third preview): `instanceof` can test primitive types, patterns can be primitive (`case int s when ...`), and `switch` finally accepts `long`, `float`, `double` and `boolean`.

```java
if (value instanceof byte b) { ... }   // "does this int fit in a byte exactly?"

return switch (enabled) {              // switch on a boolean
    case true -> "ON";
    case false -> "OFF";
};
```

> **Preview feature:** this is not final yet — the code must be compiled and run with `--enable-preview`. This project already does that in `pom.xml`, for both `maven-compiler-plugin` and `maven-surefire-plugin`. Don't use preview features in production code: they can still change between releases.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java25/primitivepatterns`](../../../src/test/java/java25/primitivepatterns) (classes under `src/main/java/java25/primitivepatterns`). Run them with `mvn test`.

---

## The key idea: `instanceof` on primitives means "converts exactly"

For reference types, `x instanceof T` asks "is this object a `T`?". For primitives there are no objects, so it asks a different, very useful question: **can this value be converted to `T` without losing information?** If yes, the pattern matches and binds the converted value.

### Before: range checks by hand

```java
public static String narrowestType(int value) {
    if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
        return "byte " + (byte) value;
    }
    if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
        return "short " + (short) value;
    }
    return "int " + value;
}
```

### After: let the language check it

```java
public static String narrowestType(int value) {
    if (value instanceof byte b) {
        return "byte " + b;
    }
    if (value instanceof short s) {
        return "short " + s;
    }
    return "int " + value;
}
```

A parameterized test proves both versions agree on every edge (`127`, `-128`, `128`, `32_767`, `Integer.MIN_VALUE`...).

### It catches precision loss, not just range

The check is about **exactness**, which matters most with floating point:

```java
3.0 instanceof int      // true  — converts exactly to 3
3.5 instanceof int      // false — would lose the .5
1e20 instanceof int     // false — out of int range

16_777_216 instanceof float   // true  — 2^24 is representable
16_777_217 instanceof float   // false — a float can't hold it exactly...
(float) 16_777_217            // ...and the plain cast silently gives 16_777_216.0
```

That last one is the kind of silent bug a cast hides and a primitive pattern exposes.

---

## Primitive patterns in `switch`

Primitive type patterns work in `switch` like reference type patterns do, including guards, and can be mixed with constant labels:

```java
public static String classifyHttpStatus(int status) {
    return switch (status) {
        case 200 -> "OK";
        case 404 -> "Not Found";
        case int s when s >= 200 && s < 300 -> "Success (" + s + ")";
        case int s when s >= 400 && s < 500 -> "Client error (" + s + ")";
        case int s when s >= 500 && s < 600 -> "Server error (" + s + ")";
        case int s -> "Unknown (" + s + ")";
    };
}
```

Before, a `switch` on `int` could only use constants, and anything range-based needed an `if/else` chain. The final `case int s` is **unconditional** (it matches every `int`), which makes the switch exhaustive without a `default`.

---

## `switch` on `long`, `float`, `double` and `boolean`

Until now, the only primitives `switch` accepted were `int` and the types that widen to it (`byte`, `short`, `char`) — on top of boxes, `String`, enums and, since Java 21, any reference type for patterns. A `long` file size had to go through `if/else`:

```java
public static String describeFileSize(long bytes) {
    return switch (bytes) {
        case 0L -> "empty";
        case long b when b < 1024 -> b + " B";
        case long b when b < 1024 * 1024 -> (b / 1024) + " KB";
        case long b -> (b / (1024 * 1024)) + " MB";
    };
}
```

And `boolean` switches are exhaustive with just the two constants:

```java
return switch (enabled) {
    case true -> "ON";
    case false -> "OFF";
};
```

---

## Primitive patterns nested in record patterns

Primitive patterns also work inside record patterns — the nested `int whole` only matches when the `double` component converts to `int` exactly:

```java
public record SensorReading(double value) {
}

public static String describe(SensorReading reading) {
    return switch (reading) {
        case SensorReading(int whole) -> "exact reading " + whole;
        case SensorReading(double approx) -> "approximate reading " + approx;
    };
}
```

```java
describe(new SensorReading(21.0));   // "exact reading 21"
describe(new SensorReading(21.5));   // "approximate reading 21.5"
```

Before JEP 507, a nested pattern had to match the component's type exactly, so `SensorReading(int whole)` didn't compile.

---

## Summary

| Before | With primitive patterns (Java 25, preview) |
|---|---|
| `instanceof` only for reference types | `value instanceof byte b` — matches if the conversion is exact |
| Manual `MIN_VALUE`/`MAX_VALUE` range checks | The language checks range **and** precision |
| Casts silently truncate or round | Patterns refuse to match when information would be lost |
| `switch` on `int` only with constants | `case int s when ...` guards, mixed with constants |
| No `switch` on `long`, `float`, `double`, `boolean` | All primitive types can be switched on |
| Record pattern components must match exactly | Nested primitive patterns: `case SensorReading(int whole)` |
