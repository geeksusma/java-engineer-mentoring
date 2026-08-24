# Java Collections

The Java Collections Framework gives you a set of interfaces and implementations for storing and manipulating groups of objects. Picking the right one is mostly about answering three questions:

1. Do I need order, uniqueness, or key-value pairs?
2. What's the Big O of the operations I do most (read, insert, delete, search)?
3. Will multiple threads touch this collection at the same time?

## The Core Interfaces

```
Collection
├── List   (ordered, allows duplicates)
├── Set    (no duplicates)
└── Queue  (FIFO / LIFO / priority processing)

Map (key-value pairs, not a Collection but part of the framework)
```

---

## List

An ordered collection that allows duplicates and index-based access.

### ArrayList

Backed by a resizable array.

```java
List<String> names = new ArrayList<>();
names.add("Alice");
names.add("Bob");
names.add("Alice"); // duplicates allowed

String first = names.get(0); // index access
```

**Big O:**
| Operation | Complexity |
|---|---|
| `get(index)` | O(1) |
| `add(element)` (at the end, amortized) | O(1) |
| `add(index, element)` | O(n) |
| `remove(index)` | O(n) |
| `contains(element)` | O(n) |

**Thread safety:** Not thread-safe. Modern alternative: `CopyOnWriteArrayList` (great for read-heavy, rarely-modified lists — every write copies the whole underlying array, so writes are expensive) or wrap with `Collections.synchronizedList(new ArrayList<>())` (coarse locking, still needs manual synchronization when iterating).

### LinkedList

Backed by a doubly linked list. Implements both `List` and `Deque`.

```java
LinkedList<String> queue = new LinkedList<>();
queue.addFirst("first");
queue.addLast("last");
String head = queue.removeFirst();
```

**Big O:**
| Operation | Complexity |
|---|---|
| `get(index)` | O(n) |
| `addFirst` / `addLast` | O(1) |
| `add(index, element)` | O(n) (traversal) + O(1) insert |
| `contains(element)` | O(n) |

**Thread safety:** Not thread-safe.

> **In the modern world:** `ArrayList` wins almost always. Cache locality of a contiguous array beats the pointer-chasing of a linked list even when the Big O favors `LinkedList` (e.g. `addFirst`). Reach for `LinkedList` mainly when you specifically need `Deque` semantics, and even then `ArrayDeque` usually performs better.

---

## Set

A collection with no duplicates.

> **Custom objects and `equals`/`hashCode`:** `HashSet` and `LinkedHashSet` decide "is this a duplicate?" by calling `hashCode()` to find the bucket and `equals()` to compare within it. If you put custom objects in without overriding both, you get Java's default identity-based behavior — two objects with the same field values are still treated as different, so duplicates you'd expect to be rejected get added anyway. `TreeSet` doesn't use `equals`/`hashCode` at all — it uses `compareTo`/`Comparator`, so uniqueness there depends on getting that comparison logic right instead. (Records generate `equals`/`hashCode` for you, which is one reason they're a good fit here.)

### HashSet

Backed by a `HashMap` internally. No guaranteed order.

```java
Set<String> tags = new HashSet<>();
tags.add("java");
tags.add("java"); // ignored, already present
```

**Big O:**
| Operation | Complexity |
|---|---|
| `add` | O(1) average, O(n) worst case (hash collisions) |
| `contains` | O(1) average, O(n) worst case |
| `remove` | O(1) average, O(n) worst case |

**Thread safety:** Not thread-safe. Modern alternative: `ConcurrentHashMap.newKeySet()` (backed by a `ConcurrentHashMap`, lock-striped, much better throughput than `Collections.synchronizedSet`).

### LinkedHashSet

Like `HashSet`, but preserves insertion order via an internal doubly linked list.

```java
Set<String> ordered = new LinkedHashSet<>();
ordered.add("c");
ordered.add("a");
ordered.add("b");
// iteration order: c, a, b
```

**Big O:** Same as `HashSet` — O(1) average for add/contains/remove, with a small constant-factor overhead to maintain the linked list.

**Thread safety:** Not thread-safe.

### TreeSet

Backed by a red-black tree (`TreeMap` internally). Keeps elements sorted.

```java
Set<Integer> sorted = new TreeSet<>();
sorted.add(5);
sorted.add(1);
sorted.add(3);
// iteration order: 1, 3, 5
```

**Big O:**
| Operation | Complexity |
|---|---|
| `add` | O(log n) |
| `contains` | O(log n) |
| `remove` | O(log n) |
| `first()` / `last()` | O(log n) |

**Thread safety:** Not thread-safe. Modern alternative: `ConcurrentSkipListSet` — a lock-free, sorted, thread-safe set with O(log n) operations.

> **In the modern world:** `HashSet` for pure uniqueness checks, `LinkedHashSet` when you also care about insertion order (e.g. deduplicating while preserving the original sequence), `TreeSet` only when you need sorted iteration or range queries (`headSet`, `tailSet`, `subSet`).

---

## Map

Key-value pairs. Not part of the `Collection` interface, but a core member of the framework.

> **Custom objects as keys:** same rule as `HashSet` — `HashMap` and `LinkedHashMap` use `hashCode()` to locate the bucket and `equals()` to match the key within it. A custom key class without both overridden means `map.get(key)` can fail to find a value you just `put()` with an equal (but not identical) key object. `TreeMap` again relies on `compareTo`/`Comparator` instead.

### HashMap

The default choice. No guaranteed order.

```java
Map<String, Integer> ages = new HashMap<>();
ages.put("Alice", 30);
ages.put("Bob", 25);
int age = ages.get("Alice");
```

**Big O:**
| Operation | Complexity |
|---|---|
| `get(key)` | O(1) average, O(n) worst case |
| `put(key, value)` | O(1) average, O(n) worst case |
| `remove(key)` | O(1) average, O(n) worst case |

Since Java 8, when a single bucket accumulates too many colliding entries (default threshold: 8), that bucket is converted from a linked list to a red-black tree, capping the worst case at O(log n) instead of O(n).

**Thread safety:** Not thread-safe — concurrent modification can even corrupt internal state (classic infinite-loop bug on resize in old JDKs). Modern alternative: `ConcurrentHashMap` — segmented/striped locking (Java 8+ uses per-bin CAS operations instead of full segment locks), supports high concurrent read/write throughput without locking the entire map. Avoid `Hashtable` and `Collections.synchronizedMap` — they're legacy, coarse-grained, and slower under contention.

### LinkedHashMap

Preserves insertion order (or access order, if configured) — perfect building block for an LRU cache.

```java
Map<String, Integer> lru = new LinkedHashMap<>(16, 0.75f, true) {
    protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
        return size() > 100; // evict oldest when capacity exceeded
    }
};
```

**Big O:** Same as `HashMap` — O(1) average, with linked-list bookkeeping overhead.

**Thread safety:** Not thread-safe.

### TreeMap

Backed by a red-black tree. Keeps keys sorted.

```java
Map<String, Integer> sorted = new TreeMap<>();
sorted.put("banana", 2);
sorted.put("apple", 1);
// iteration order: apple, banana
```

**Big O:**
| Operation | Complexity |
|---|---|
| `get(key)` | O(log n) |
| `put(key, value)` | O(log n) |
| `remove(key)` | O(log n) |
| `firstKey()` / `lastKey()` | O(log n) |

**Thread safety:** Not thread-safe. Modern alternative: `ConcurrentSkipListMap` — thread-safe, sorted, O(log n) operations, no locking (skip-list based).

> **In the modern world:** `HashMap` by default, `ConcurrentHashMap` the instant more than one thread touches it, `LinkedHashMap` for predictable iteration order or LRU caches, `TreeMap` only when sorted keys or range queries genuinely matter.

---

## Queue / Deque

Mentioned for completeness — `Queue`/`Deque` (`ArrayDeque`, `PriorityQueue`) aren't reached for that often in everyday application code, since most real-world producer/consumer and ordering needs are handled by `java.util.concurrent` types (`LinkedBlockingQueue`, `ArrayBlockingQueue`) or by a messaging system (Kafka, RabbitMQ, SQS) instead. Worth knowing they exist and that `ArrayDeque` replaced the legacy `Stack`, but not worth deep-diving here.

---

## Quick Reference: Thread-Safe Alternatives

| Sequential (not thread-safe) | Concurrent alternative |
|---|---|
| `ArrayList` | `CopyOnWriteArrayList` |
| `HashSet` | `ConcurrentHashMap.newKeySet()` |
| `TreeSet` | `ConcurrentSkipListSet` |
| `HashMap` | `ConcurrentHashMap` |
| `TreeMap` | `ConcurrentSkipListMap` |
| `ArrayDeque` | `ConcurrentLinkedDeque` / `LinkedBlockingDeque` |
| `PriorityQueue` | `PriorityBlockingQueue` |

Avoid the old `synchronized` wrappers (`Vector`, `Hashtable`, `Collections.synchronizedXxx`) in new code — they lock the entire structure on every operation, so they don't scale under contention the way the `java.util.concurrent` collections do. They also don't make compound operations (check-then-act, iterate-then-modify) safe; you still need external synchronization or an atomic method (`computeIfAbsent`, `putIfAbsent`) for those.

## Summary Table

| Collection | Ordering | Duplicates | Access | Insert | Search | Thread-safe version |
|---|---|---|---|---|---|---|
| `ArrayList` | Insertion | Yes | O(1) | O(1) amortized | O(n) | `CopyOnWriteArrayList` |
| `LinkedList` | Insertion | Yes | O(n) | O(1) at ends | O(n) | — |
| `HashSet` | None | No | — | O(1) avg | O(1) avg | `ConcurrentHashMap.newKeySet()` |
| `LinkedHashSet` | Insertion | No | — | O(1) avg | O(1) avg | — |
| `TreeSet` | Sorted | No | — | O(log n) | O(log n) | `ConcurrentSkipListSet` |
| `HashMap` | None | Keys unique | O(1) avg | O(1) avg | O(1) avg | `ConcurrentHashMap` |
| `LinkedHashMap` | Insertion/access | Keys unique | O(1) avg | O(1) avg | O(1) avg | — |
| `TreeMap` | Sorted | Keys unique | O(log n) | O(log n) | O(log n) | `ConcurrentSkipListMap` |
| `ArrayDeque` | Insertion | Yes | — | O(1) amortized | O(n) | `ConcurrentLinkedDeque` |
| `PriorityQueue` | Priority | Yes | — | O(log n) | O(n) | `PriorityBlockingQueue` |
