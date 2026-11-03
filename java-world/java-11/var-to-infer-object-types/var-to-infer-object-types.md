# var to infer object types

Java 10 introduced `var` for local variable type inference: the compiler figures out the concrete type from the right-hand side of an assignment, so the declaration doesn't have to spell it out. Java 11 extended where `var` can be used, most notably to **lambda parameters**. `var` is not "loose" or dynamic typing — the compiler still assigns a single, fixed, concrete type at compile time; it just infers what that type is instead of requiring it to be written out.

```java
// Instead of:
Map<String, Integer> lengths = new LinkedHashMap<>();

// Java 10+:
var lengths = new LinkedHashMap<String, Integer>();

// Java 11+: var is also legal on lambda parameters
Function<String, String> toUpper = (var word) -> word.toUpperCase();
```

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java11/varinference`](../../../src/test/java/java11/varinference) (`VarShowcase` under `src/main/java/java11/varinference`). Run them with `mvn test`.

---

## `var` in loops and local declarations

`var` works anywhere a local variable is declared with an initializer, including enhanced `for` loops:

```java
public static int sumLengthsWithEnhancedFor(List<String> words) {
    var total = 0;
    for (var word : words) {
        total += word.length();
    }
    return total;
}
```

Here `total` is inferred as `int` and `word` as `String` — the compiler reads the initializer (`0`) and the loop source (`List<String>`) to determine each type, once, at compile time.

---

## `var` infers the concrete type, not the declared interface

A common misconception is that `var` somehow makes code "less typed." In fact it infers the *most specific* type available — the actual concrete type of the right-hand side, not whatever interface it might later be assigned to:

```java
public static Map<String, Integer> lengthsByWord(List<String> words) {
    var lengths = new LinkedHashMap<String, Integer>();
    for (var word : words) {
        lengths.put(word, word.length());
    }
    return lengths;
}
```

```java
Map<String, Integer> lengths = VarShowcase.lengthsByWord(List.of("Ana", "Bob"));

assertInstanceOf(LinkedHashMap.class, lengths);
```

Had this been written as `Map<String, Integer> lengths = new LinkedHashMap<>();`, the *variable's* declared type would be `Map`. With `var`, the variable's type is the narrower `LinkedHashMap<String, Integer>` — everything `LinkedHashMap` offers beyond the `Map` interface (like `LinkedHashMap`-specific behavior) stays visible on that variable.

---

## `var` on lambda parameters (Java 11)

Java 10 didn't allow `var` on a lambda parameter. Java 11 added it:

```java
public static List<String> upperCaseAll(List<String> words) {
    Function<String, String> toUpper = (var word) -> word.toUpperCase();
    return words.stream().map(toUpper).toList();
}
```

On its own this reads like a strange detour — plain `word -> word.toUpperCase()` is shorter. The reason `var` was allowed here at all is to let lambda parameters carry **annotations**, which unannotated inferred parameters (`word -> ...`) cannot:

```java
(@NonNull var word) -> word.toUpperCase();
```

Without `var`, annotating an inferred lambda parameter isn't legal syntax — you'd have to fall back to the fully explicit `(@NonNull String word) -> ...`, losing the inference. `var` closes that gap.

---

## `var` and anonymous classes

This is where `var` does something no explicit type ever could: it preserves the *full* type of an anonymous class, including members that don't exist on any named supertype.

```java
public static String describeUsingAnonymousClassExtraMember(String value) {
    var describer = new Object() {
        String reversed() {
            return new StringBuilder(value).reverse().toString();
        }
    };
    // Only possible because `var` kept the anonymous class's real type.
    // Declaring `Object describer = ...` would hide reversed() entirely.
    return describer.reversed();
}
```

`new Object() { String reversed() { ... } }` creates an anonymous subclass of `Object` with an extra method, `reversed()`, that doesn't exist on `Object` itself. There is no way to *name* that anonymous class's type — so before `var`, the only legal declaration was `Object describer = ...`, which hides `reversed()` completely. `var` lets the compiler infer the anonymous class's own unnamed type and keep `reversed()` callable.

---

## Summary

| Feature | Introduced in | What changed |
|---|---|---|
| `var` for local variables | Java 10 | Compiler infers the declared type from the initializer |
| `var` on lambda parameters | Java 11 | Lets lambda parameters carry annotations while still being inferred |
| `var` preserving anonymous class members | Java 10 (applies wherever `var` is legal) | The only way to keep an anonymous class's extra members accessible on a local variable |

| Misconception | Reality |
|---|---|
| "`var` makes Java dynamically typed" | The type is still fixed at compile time — `var` only omits writing it out |
| "`var` widens the type to the declared interface" | `var` infers the *narrowest* concrete type of the initializer |
| "`var` is only sugar for shorter code" | It's also the only way to keep an anonymous class's extra members usable |
