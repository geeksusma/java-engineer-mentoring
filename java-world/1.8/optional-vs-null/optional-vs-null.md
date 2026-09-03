# `Optional` vs. `null`

Before Java 8, "this method might not have a result" was communicated by convention only: it might return `null`, throw an exception, or return a sentinel value — and nothing in the method signature told the caller which. `Optional<T>` makes "might be absent" part of the **type**, so the compiler and the reader both see it at the call site.

```java
// Before: does this ever return null? You have to read the implementation (or the docs, if they exist) to know.
User findById(long id);

// After: the signature itself says "you might get nothing back"
Optional<User> findById(long id);
```

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java8/optional`](../../../src/test/java/java8/optional) (classes under `src/main/java/java8/optional`). Run them with `mvn test`.

---

## The anti-pattern: `isPresent()` + `get()`

The single most common way people misuse `Optional` is treating it as a fancier null check:

```java
Optional<User> user = repository.findById(id);
if (user.isPresent()) {
    return user.get().email();
} else {
    return "unknown@example.com";
}
```

This is functionally identical to a null check — it just wraps the same branching logic in an extra object. It doesn't gain you anything `if (user != null)` didn't already give you, and it's easy to forget the `isPresent()` guard and call `.get()` directly, which throws `NoSuchElementException` on an empty `Optional` — the exact failure mode `Optional` exists to help you avoid.

The idiomatic version chains the "what to do if present" logic instead of branching on presence:

```java
String email = repository.findById(id)
        .map(User::email)
        .orElse("unknown@example.com");
```

---

## `map` / `flatMap` / `filter`

`Optional` supports the same functional vocabulary as `Stream`, treating "present" as a stream of zero or one elements:

```java
public Optional<User> findById(long id) {
    return users.stream()
            .filter(u -> u.id() == id)
            .findFirst();
}

public Optional<String> findEmail(long id) {
    return findById(id).map(User::email); // transforms the value IF present, stays empty otherwise
}
```

Use `flatMap` instead of `map` when the transformation itself returns an `Optional` — otherwise you end up with `Optional<Optional<T>>`:

```java
Optional<User> user = findById(id);
Optional<Address> address = user.flatMap(User::primaryAddress); // primaryAddress() returns Optional<Address>
```

`filter` narrows a present value down to empty if it doesn't match a predicate — useful for validation chains without an explicit `if`:

```java
Optional<User> adminUser = findById(id).filter(User::isAdmin);
```

---

## `orElse` vs. `orElseGet`: eager vs. lazy

This is the sharpest edge in the API. `orElse(T)` takes an **already-computed value** — its argument is evaluated every single time, whether the `Optional` is present or not. `orElseGet(Supplier<T>)` takes a **function** that is only invoked when the `Optional` is actually empty.

```java
AtomicInteger callCount = new AtomicInteger();

Optional.of("present").orElse(expensiveDefault(callCount));   // callCount becomes 1 anyway!
Optional.of("present").orElseGet(() -> expensiveDefault(callCount)); // callCount stays 0
```

If the fallback is a constant (`"unknown@example.com"`, `List.of()`), `orElse` is fine and reads slightly cleaner. If the fallback involves a method call — a database query, building an object, logging — use `orElseGet`, or you'll pay for that call on every present path too.

---

## Handling absence

| Method | Behavior when empty |
|---|---|
| `orElse(value)` | Returns `value` |
| `orElseGet(supplier)` | Returns `supplier.get()` — computed lazily |
| `orElseThrow()` | Throws `NoSuchElementException` |
| `orElseThrow(supplier)` | Throws whatever exception `supplier` builds |
| `ifPresent(consumer)` | Runs `consumer` only if present, otherwise does nothing |
| `ifPresentOrElse(consumer, runnable)` | Runs one or the other, depending on presence |

```java
User user = repository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("No user with id " + id));
```

---

## Creating an `Optional`

- `Optional.of(value)` — throws `NullPointerException` immediately if `value` is `null`. Use only when you're certain the value can't be null.
- `Optional.ofNullable(value)` — wraps `value`, producing `Optional.empty()` if it's `null`. This is what you reach for when adapting a nullable API (e.g. a legacy method, a `Map.get`) into `Optional`.
- `Optional.empty()` — an explicitly empty instance.

---

## What `Optional` is *not* for

- **Fields.** `Optional` doesn't implement `Serializable` and adds an extra allocation/indirection for no benefit over a plain nullable field with a documented default. Model "this field might be absent" with `null` (or, better, don't allow the invalid state to exist in the first place) and reserve `Optional` for method return types.
- **Method parameters — it's a flag argument in disguise.** `void process(Optional<Config> config)` just pushes the null-check problem into the method body, and it's the same code smell as a boolean flag argument: the caller is forced to manufacture a wrapper value (`Optional.empty()` or `Optional.of(x)`) just to select which internal branch the method should take, and *every* caller pays that wrapping cost — including the common case, which is usually "nothing."

  ```java
  // Anti-pattern: the caller has to wrap "nothing" or wrap "something" just to call this
  public void sendNotification(User user, Optional<String> customMessage) {
      String message = customMessage.orElseGet(() -> defaultMessage(user));
      // ...
  }

  sendNotification(user, Optional.empty());
  sendNotification(user, Optional.of("Welcome back!"));
  ```

  Prefer an overload instead — let the *absence of an argument* do what `Optional.empty()` was doing, so the common case doesn't wrap anything at all, and the method body doesn't need to unwrap a value on its very first line:

  ```java
  public void sendNotification(User user) {
      sendNotification(user, defaultMessage(user));
  }

  public void sendNotification(User user, String customMessage) {
      // ...
  }
  ```

  This is also why `Optional` doesn't belong on constructors or setters for the same reason — if a value is genuinely optional at construction time, an overloaded constructor/builder step communicates that more directly than a parameter type the caller has to wrap.
- **Collections.** Don't return `Optional<List<T>>` — an empty `List` already communicates "nothing here" without an extra wrapping layer. Reserve `Optional` for genuinely single-valued results.

**In the modern world:** `Optional` is a *return-type* tool for "this lookup/computation may legitimately have no result," used at the boundary where the caller needs to react to that possibility. It doesn't replace `null` everywhere, and it doesn't replace proper input validation — it replaces the specific pattern of "returns null with no signature-level warning, caller forgets to check."

---

## Summary

| | `null` | `Optional<T>` |
|---|---|---|
| Visible in the method signature? | No — you have to read the implementation or docs | Yes — the return type says "might be absent" |
| Forgetting to check | `NullPointerException`, often far from the real cause | `NoSuchElementException` on `.get()`, or compiles away entirely if you use `map`/`orElse` |
| Chaining transformations | Manual null checks at every step | `map`/`flatMap`/`filter` short-circuit automatically on empty |
| Appropriate for fields/parameters? | Yes | No — return types only |
