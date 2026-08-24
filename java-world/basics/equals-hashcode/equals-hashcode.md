# `equals()` and `hashCode()`

Every Java object inherits `equals(Object)` and `hashCode()` from `Object`. By default:

- `equals()` compares references (`this == obj`) — two objects are only "equal" if they're the exact same instance in memory.
- `hashCode()` returns a number derived from the object's memory address (in practice, the JVM's identity hash).

That's rarely what you want for a class like `Point`, `User`, or `Money`, where two separate instances with the same field values should be treated as "the same thing." That's why you override both.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/basics/equalshashcode`](../../../src/test/java/basics/equalshashcode) (classes under `src/main/java/basics/equalshashcode`). Run them with `mvn test`.

---

## Why they matter

If you override `equals()` without overriding `hashCode()` (or vice versa), you break the **contract** that hash-based collections (`HashMap`, `HashSet`, `HashSet`-backed things like `HashMap.keySet()`) rely on. Symptoms:

```java
record Point(int x, int y) {} // records get this right automatically — see note below

class BadPoint {
    int x, y;
    BadPoint(int x, int y) { this.x = x; this.y = y; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BadPoint p)) return false;
        return x == p.x && y == p.y;
    }
    // hashCode() NOT overridden — still identity-based
}

Set<BadPoint> set = new HashSet<>();
set.add(new BadPoint(1, 2));
set.contains(new BadPoint(1, 2)); // false! different hashCode -> different bucket, equals() never even called
```

Two objects that are logically equal end up in different hash buckets, so `HashSet.contains`, `HashMap.get`, deduplication, and anything else relying on hashing silently breaks. This is the single most common cause of "why isn't my object being found in the map/set even though I know it's the same value" bugs.

**In the modern world:** use `record` for simple data carriers — it generates correct `equals()`/`hashCode()`/`toString()` from the components for you. For regular classes, generate them with your IDE rather than hand-writing them; it's easy to get subtly wrong (forgetting a field, using `==` on boxed types, etc.).

---

## The contract (from the `Object` Javadoc)

1. **Consistency**: calling `hashCode()` multiple times on the same object (without modifying the fields used in `equals`) must return the same value.
2. **If `a.equals(b)` is `true`, then `a.hashCode() == b.hashCode()` must also be true.** Equal objects *must* produce the same hash code.
3. **The reverse is *not* required.** Two objects can have the same `hashCode()` and still be unequal. This is called a **hash collision**, and it's expected and normal — not a bug.

Point 3 is the one that trips people up, so it's worth stating plainly: **same hashCode does not mean equal.** It only means "candidates worth checking with `equals()`." The only guaranteed direction is `equals → same hashCode`, never the other way around.

---

## Why can two different (unequal) objects have the same hashCode?

Because `hashCode()` returns an `int` — about 4.3 billion possible values — but the space of possible objects (all possible strings, all possible `User` instances, etc.) is effectively infinite. By the **pigeonhole principle**, you cannot map an infinite (or larger-than-4.3-billion) input space onto a fixed 32-bit output space without some inputs landing on the same output.

```java
"Aa".hashCode();  // 2112
"BB".hashCode();  // 2112 — same hash, different (unequal) strings
"Aa".equals("BB"); // false
```

A good `hashCode()` implementation just tries to **spread values out as evenly as possible** to make collisions rare — it can never eliminate them entirely.

---

## Why is this needed for `HashMap`'s implementation?

A `HashMap` doesn't scan every entry to find a key — that would be O(n). Instead it uses the hash code to jump almost straight to the right spot:

1. **Bucketing.** The map has an internal array of buckets (default capacity 16, doubling as it grows). On `put`/`get`, it computes `hash(key.hashCode())` and maps that to a bucket index (roughly `hash & (capacity - 1)`). This is what makes `get`/`put` O(1) on average — you go directly to a bucket instead of checking every entry.
2. **Collisions within a bucket.** Since different keys can land in the same bucket (hash collision, or just because `capacity` is much smaller than 2^32 so many hash codes fold onto the same index), each bucket holds a small list (or, since Java 8, a red-black tree once a bucket gets too full — 8+ entries) of all the entries that hashed there.
3. **Disambiguating within a bucket via `equals()`.** Once the map has narrowed things down to "these N entries share a bucket," it must call `equals()` on each candidate key to find the *actual* match. This is exactly why rule 2 of the contract matters: if two equal keys had different hash codes, they could end up in different buckets and `get()` would never find the one you `put()`.

```
put("Aa", 1)     hashCode("Aa") = 2112  -> bucket 2112 % capacity
put("BB", 2)     hashCode("BB") = 2112  -> SAME bucket (collision)

get("Aa")
  -> jump to that bucket (O(1))
  -> bucket has 2 entries: ("Aa", 1) and ("BB", 2)
  -> walk the bucket, call equals() on each key
  -> "Aa".equals("Aa") -> true  -> return 1
  -> "Aa".equals("BB") -> false -> skip
```

So the two methods split the work:

| Method | Job | Cost |
|---|---|---|
| `hashCode()` | Narrow billions of possible keys down to *one bucket* | O(1) |
| `equals()` | Find the exact match among the (usually few) keys in that bucket | O(bucket size), typically O(1) |

If `hashCode()` is broken (e.g., always returns `0`), the map still works correctly — everything just lands in one giant bucket, degrading `HashMap` to a linked list, O(n) lookups. If `equals()` is broken or missing, the map returns wrong results entirely, because it can't tell two candidates apart once they share a bucket. **Both are required, and they must agree with each other**, which is exactly what the contract in rule 2 enforces.
