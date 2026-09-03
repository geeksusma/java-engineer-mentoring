# Streams API

A `Stream` is not a data structure. It doesn't store elements — it's a **pipeline** that describes a computation over a source (a collection, an array, a generator function...), computed on demand when you ask for a result.

```java
List<String> names = List.of("Ana", "Bob", "Carl", "Dana");

List<String> shortUpperNames = names.stream()
        .filter(n -> n.length() <= 3)
        .map(String::toUpperCase)
        .sorted()
        .toList();
// [ANA, BOB]
```

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java8/streams`](../../../src/test/java/java8/streams) (classes under `src/main/java/java8/streams`). Run them with `mvn test`.

---

## Anatomy of a pipeline

Every stream pipeline has three parts:

1. **A source** — `collection.stream()`, `Stream.of(...)`, `Stream.iterate(...)`, `IntStream.range(...)`...
2. **Zero or more intermediate operations** — `filter`, `map`, `sorted`, `distinct`, `limit`, `peek`... Each returns a *new* stream. They are **lazy**: calling them does not process any element.
3. **Exactly one terminal operation** — `toList`, `collect`, `reduce`, `forEach`, `findFirst`, `count`, `anyMatch`... This is what actually pulls elements through the pipeline. Without it, nothing runs.

```java
Stream<String> pipeline = names.stream()
        .filter(n -> n.length() <= 3)   // nothing executed yet
        .map(String::toUpperCase);      // still nothing executed

pipeline.toList(); // NOW the whole pipeline runs, element by element
```

---

## Laziness: nothing happens until the terminal operation

Because intermediate operations are lazy, elements are pulled through the *entire* pipeline one at a time (filter → map → next filter → ...) rather than the stream running each stage to completion before starting the next. You can prove this by attaching a side effect (`peek`) and checking a counter before and after the terminal call:

```java
AtomicInteger mapCalls = new AtomicInteger();

Stream<String> pipeline = names.stream()
        .map(n -> {
            mapCalls.incrementAndGet();
            return n.toUpperCase();
        });

assertEquals(0, mapCalls.get()); // map() hasn't run a single time yet

pipeline.toList();

assertEquals(names.size(), mapCalls.get()); // now it has
```

---

## Short-circuiting: some operations stop early

`findFirst`, `findAny`, `anyMatch`, `allMatch`, `noneMatch`, and `limit` don't need to process every element to produce a result — they stop as soon as they know the answer:

```java
List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

Optional<Integer> firstEven = numbers.stream()
        .peek(n -> System.out.println("checking " + n))
        .filter(n -> n % 2 == 0)
        .findFirst();
// prints "checking 1", "checking 2" — then STOPS. 3 through 10 are never visited.
```

This also means a stream can be built on top of an **infinite** source, as long as a short-circuiting operation eventually cuts it off:

```java
List<Integer> firstFivePowersOfTwo = Stream.iterate(1, n -> n * 2)
        .limit(5)
        .toList();
// [1, 2, 4, 8, 16] — Stream.iterate never terminates on its own
```

---

## Streams are single-use

A stream models a *single* walk over its source. Once a terminal operation has been called, that stream is spent — calling any operation on it again throws `IllegalStateException`:

```java
Stream<String> stream = names.stream();
stream.toList();
stream.toList(); // IllegalStateException: stream has already been operated upon or closed
```

If you need to run the pipeline twice, build it twice from the source (`names.stream()` again), or capture the result in a `List` and reuse that.

---

## `map` / `filter` / `reduce` in practice

```java
public record Product(String name, String category, BigDecimal price, int quantity) {}

public static BigDecimal totalValue(List<Product> products) {
    return products.stream()
            .map(p -> p.price().multiply(BigDecimal.valueOf(p.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
}
```

- `filter(Predicate)` — keep elements matching a condition.
- `map(Function)` — transform each element into something else (1-to-1).
- `reduce(identity, BinaryOperator)` — combine all elements into a single value.

---

## Streams run on functional interfaces

`filter` doesn't accept "a lambda" as special stream syntax — its parameter is typed as `Predicate<T>`, an ordinary interface with exactly one abstract method. A lambda is just the shorthand Java gives you for implementing that one method inline. Every stream operation is typed against one of these:

| Stream method | Functional interface | Abstract method |
|---|---|---|
| `filter` | `Predicate<T>` | `boolean test(T t)` |
| `map` | `Function<T, R>` | `R apply(T t)` |
| `forEach`, `peek` | `Consumer<T>` | `void accept(T t)` |
| `reduce` | `BinaryOperator<T>` | `T apply(T t1, T t2)` |
| `Stream.generate` | `Supplier<T>` | `T get()` |
| `sorted(Comparator)` | `Comparator<T>` | `int compare(T a, T b)` |

Because it's a real interface, three different things can be passed to `filter`, and the pipeline can't tell them apart:

```java
// 1. Lambda expression — the common case
products.stream().filter(p -> p.category().equals("Displays"));

// 2. Method reference — when the logic already exists as a method
products.stream().filter(this::isExpensive);

// 3. A named variable of the interface type — build it once, reuse it
Predicate<Product> isDisplay = p -> p.category().equals("Displays");
products.stream().filter(isDisplay);
```

### The "empty" (no-op) lambda

A stream pipeline's shape is fixed once you write it — there's no way to tell `filter` "skip this stage" for a given call. When a stage should conditionally do nothing, you satisfy the functional interface with a lambda that ignores its argument, instead of writing two separate pipelines:

```java
public static List<String> namesInCategoryOrAll(List<Product> products, String category) {
    Predicate<Product> categoryFilter = category == null
            ? p -> true               // no-op predicate — keep everything
            : p -> p.category().equals(category);

    return products.stream()
            .filter(categoryFilter)
            .map(Product::name)
            .toList();
}
```

- A **no-op `Predicate`**, `p -> true`, keeps every element — the pattern above.
- A **no-op `Consumer`**, `p -> {}`, is the same idea for `peek`/`forEach` — most often seen in tests, where `peek` exists purely to count visits rather than to do real work.

There's nothing syntactically special about either one: `p -> true` and `p -> {}` are ordinary lambdas whose bodies happen to ignore the argument. It comes up specifically with streams because a no-op implementation is how you make one stage "invisible" for a particular call, rather than duplicating the whole pipeline behind an `if`/`else`.

---

## Collectors: turning a stream back into something useful

`collect(Collector)` is the general-purpose terminal operation for building a result other than a simple list:

```java
// Group products by category
Map<String, List<Product>> byCategory = products.stream()
        .collect(Collectors.groupingBy(Product::category));

// Join names into a single string
String csv = products.stream()
        .map(Product::name)
        .collect(Collectors.joining(", "));
```

`toList()` (Java 16+) is shorthand for the common `collect(Collectors.toList())` case and returns an unmodifiable list.

---

## Parallel streams: a footgun, not a free lunch

`.parallelStream()` (or `.stream().parallel()`) splits the source across the common `ForkJoinPool` and processes chunks concurrently. It only pays off for **large, CPU-bound, stateless** workloads. It actively breaks things when:

- The pipeline has **shared mutable state** (e.g. accumulating into a plain `ArrayList` from `forEach` instead of using `collect`) — this is a data race, not just "slower than expected."
- Elements must be processed **in order** — use `forEachOrdered` if order matters, at the cost of losing most of the parallelism benefit.
- The source or workload is small — thread coordination overhead dwarfs any gain.

**In the modern world:** reach for a stream when you're *querying or transforming* a collection declaratively (filter this, map that, collect into this shape) — it reads closer to the intent than a manual loop with an accumulator variable. Keep the classic `for` loop when you need early `break`/`continue` with complex control flow, or when the body has multiple unrelated side effects — forcing that into `.forEach()` just to "use streams" makes it harder to read, not easier. Only reach for `.parallelStream()` when you've actually measured a CPU-bound bottleneck; it is not a default.

---

## Summary

| Operation | Kind | Short-circuits? |
|---|---|---|
| `filter`, `map`, `sorted`, `distinct`, `peek` | Intermediate (lazy) | No |
| `limit` | Intermediate (lazy) | Yes |
| `collect`, `toList`, `reduce`, `count`, `forEach` | Terminal (eager) | No |
| `findFirst`, `findAny`, `anyMatch`, `allMatch`, `noneMatch` | Terminal (eager) | Yes |

| Concept | Key fact |
|---|---|
| Source of truth | A stream is a pipeline description, not a container — the underlying collection is untouched |
| Laziness | Intermediate operations run only when a terminal operation pulls elements through |
| Reusability | A stream can be consumed exactly once; build a new one (or cache a `List`) to iterate again |
| Parallelism | Opt-in, and only safe with stateless operations and no ordering requirement |
