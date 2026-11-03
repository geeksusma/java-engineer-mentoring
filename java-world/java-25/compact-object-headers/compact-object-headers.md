# Compact Object Headers (less CPU needed)

Java 25 made **Compact Object Headers** a product feature (JEP 519, after being experimental in Java 24 with JEP 450). It shrinks the header that **every single object** on the heap carries from 12 bytes to 8 bytes. That sounds tiny, but most objects in real applications are small, so the header is a big share of total memory. Less memory means fewer GC cycles and better CPU cache usage — hence **less CPU needed**.

It's **not on by default** yet. It's one JVM flag, with **no code changes**:

```
java -XX:+UseCompactObjectHeaders -jar app.jar
```

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java25/compactheaders`](../../../src/test/java/java25/compactheaders) (`HeaderFootprint` under `src/main/java/java25/compactheaders`). Run them with `mvn test`.

---

## What's an object header?

Every object in HotSpot starts with a header the JVM uses for bookkeeping, before any of your fields:

- The **mark word** (8 bytes): identity hash code, GC age, and locking state.
- The **class pointer** (4 bytes with compressed class pointers, the default): which class this object is an instance of.

Then come the fields, and the total is rounded up to a multiple of **8 bytes** (object alignment).

```
Legacy header (12 bytes)                     Compact header (8 bytes)
+-------------------+----------+             +-------------------------------+
|  mark word (8 B)  | class 4B |             | mark word + class pointer (8B)|
+-------------------+----------+             +-------------------------------+
```

Compact headers squeeze the class pointer *into* the mark word (as a 22-bit compressed class index), so the whole header fits in 8 bytes.

---

## Proving it: measuring real allocation sizes

`HeaderFootprint` allocates one million instances and asks the JVM exactly how many bytes the current thread allocated, using `com.sun.management.ThreadMXBean.getCurrentThreadAllocatedBytes()`:

```java
var holder = new Object[INSTANCES];                  // allocated before measuring
long before = threads.getCurrentThreadAllocatedBytes();
for (int i = 0; i < INSTANCES; i++) {
    holder[i] = factory.get();
}
long after = threads.getCurrentThreadAllocatedBytes();
return Math.round((double) (after - before) / holder.length);
```

The header layout is fixed at JVM startup, so the test runs it in two child JVMs:

| Object | `-XX:-UseCompactObjectHeaders` | `-XX:+UseCompactObjectHeaders` | Saving |
|---|---|---|---|
| `new Object()` | 12 header → padded to **16 bytes** | 8 header → **8 bytes** | 50% |
| `record Point(int x, int y)` | 12 + 8 = 20 → padded to **24 bytes** | 8 + 8 = **16 bytes** | 33% |

Notice how the saving can be bigger than 4 bytes: for `Point`, those 4 bytes were exactly what pushed it past a 16-byte boundary and forced 4 more bytes of padding.

---

## Why this translates into less CPU

- **Smaller heap footprint** → the same live data fits in less memory, or the same heap holds more.
- **Fewer GC cycles** → the young generation fills up more slowly, so the collector runs less often.
- **Better cache locality** → more objects fit in each CPU cache line, so the CPU waits less on main memory.

The JEP reports, in one SPECjbb2015 setting, **22% less heap** and **8% less CPU time**. Your mileage depends on how many small objects your application allocates — typical Java services (lots of `String`s, boxed values, small DTOs, collection nodes) are exactly the profile that benefits.

---

## Should I turn it on?

- It's a supported product feature in Java 25 — no `-XX:+UnlockExperimentalVMOptions` needed any more.
- It's opt-in because it's new; it is expected to become the default in a future release.
- The approach is the usual one: enable it in a staging environment, compare memory, GC frequency, and CPU with GC logs and your metrics dashboards, then roll it out.

Your code can't tell the difference — unless it relies on exact object sizes (e.g. hand-made memory estimations or `sun.misc.Unsafe` offsets), which normal application code never should.

---

## Summary

| | Legacy headers | Compact headers |
|---|---|---|
| Header size (64-bit JVM) | 12 bytes (8 mark + 4 class) | 8 bytes (class pointer inside the mark word) |
| `new Object()` | 16 bytes | 8 bytes |
| Small object with 2 `int`s | 24 bytes | 16 bytes |
| Enabled by | Default | `-XX:+UseCompactObjectHeaders` (Java 25, JEP 519) |
| Code changes | — | None |
