# Garbage Collector enhancements

Between Java 8 (2014) and Java 17 (2021), garbage collection changed more than in any previous period. The headline: **pause times went from "hundreds of milliseconds" to "under a millisecond"** for applications that choose the right collector — without code changes. Most of this happened in the releases between the two LTS versions, which is why teams that jumped straight from 8 to 17 felt it all at once.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java17/gc`](../../../src/test/java/java17/gc) (`GcInspector` under `src/main/java/java17/gc`). Run them with `mvn test`.

---

## A 60-second GC refresher

Java allocates objects on the heap and never asks you to free them. The **garbage collector** periodically finds objects nothing references any more and reclaims their memory. Two properties matter when picking one:

- **Throughput** — what share of CPU time goes to your code rather than to GC.
- **Latency (pause time)** — how long your application threads are stopped while the GC works (*stop-the-world* pauses).

You usually trade one for the other. Most collectors are **generational**: they bet that most objects die young, so they collect the young generation often and cheaply and the old generation rarely.

---

## The collectors in Java 17

| Collector | Flag | Optimizes for | Status in 17 |
|---|---|---|---|
| **G1** (Garbage First) | `-XX:+UseG1GC` | Balance of throughput and predictable pauses | **Default** since Java 9 |
| **ZGC** | `-XX:+UseZGC` | Ultra-low latency, huge heaps (up to 16 TB) | Production-ready since 15 |
| **Shenandoah** | `-XX:+UseShenandoahGC` | Ultra-low latency | Production-ready since 15 (not in every vendor's build) |
| **Parallel** | `-XX:+UseParallelGC` | Raw throughput (batch jobs) | Default in Java 8 |
| **Serial** | `-XX:+UseSerialGC` | Tiny heaps, single CPU, containers with 1 core | Still chosen automatically on small machines |
| ~~CMS~~ | ~~`-XX:+UseConcMarkSweepGC`~~ | — | **Removed** in Java 14 (JEP 363) |

---

## What actually improved from 8 to 17

**The default changed.** Java 8 defaulted to Parallel GC (great throughput, long pauses). Java 9 switched to G1 (JEP 248), so most services got shorter, more predictable pauses just by upgrading.

**G1 got much better:**
- Parallel full GC (Java 10, JEP 307) — the worst-case fallback no longer runs single-threaded.
- Abortable mixed collections (Java 12, JEP 344) — G1 can stop part-way through to respect its pause-time goal (`-XX:MaxGCPauseMillis`, default 200 ms).
- Promptly return unused memory to the OS (Java 12, JEP 346) — great for containers billed by memory.
- NUMA-aware allocation (Java 14, JEP 345).

**Low-latency collectors became production-ready:**
- ZGC (experimental in 11, production in 15 — JEP 377) and Shenandoah (production in 15 — JEP 379) do almost all their work **concurrently** with your application.
- ZGC concurrent thread-stack processing (Java 16, JEP 376) removed the last significant pause, bringing pauses **below 1 ms regardless of heap size**.

**Metaspace:** elastic metaspace (Java 16, JEP 387) returns unused class-metadata memory to the OS faster.

**Container awareness** (backported to 8u191, on by default since 10): the JVM reads cgroup CPU and memory limits, so heap size and GC thread counts respect the container instead of the host.

And the story continued after 17: generational ZGC (Java 21, JEP 439), made the only ZGC mode in Java 24 (JEP 490).

---

## Proving which collector is running

Each collector registers `GarbageCollectorMXBean`s named after itself, so a JVM can tell you which GC it's using:

```java
public static List<String> activeCollectorNames() {
    return ManagementFactory.getGarbageCollectorMXBeans().stream()
            .map(GarbageCollectorMXBean::getName)
            .toList();
}
```

The GC is fixed at JVM startup, so the tests launch a **fresh child JVM** per flag (via the small `support.ChildJvm` test helper) and read what it reports. On Java 25 you get:

| Flag | MXBean names |
|---|---|
| `-XX:+UseSerialGC` | `Copy`, `MarkSweepCompact` |
| `-XX:+UseParallelGC` | `PS Scavenge`, `PS MarkSweep` |
| `-XX:+UseG1GC` | `G1 Young Generation`, `G1 Concurrent GC`, `G1 Old Generation` |
| `-XX:+UseZGC` | `ZGC Minor Cycles`, `ZGC Minor Pauses`, `ZGC Major Cycles`, `ZGC Major Pauses` |

And asking for CMS doesn't even start the JVM:

```
$ java -XX:+UseConcMarkSweepGC -version
Unrecognized VM option 'UseConcMarkSweepGC'
Error: Could not create the Java Virtual Machine.
```

If you migrate a service from Java 8 and its startup script still has CMS flags, this is the error you'll see.

You can check your own service without code, too:

```
java -XX:+PrintCommandLineFlags -version        # shows the chosen GC flag
jcmd <pid> GC.heap_info                          # on a running JVM
java -Xlog:gc -jar app.jar                       # unified GC logging (Java 9+)
```

---

## Which one should I use?

1. **Start with the default (G1).** It's the right choice for the vast majority of services.
2. **Latency-sensitive with a big heap** (p99 matters, heap of several GB): try **ZGC**.
3. **Batch job** where only total run time matters: try **Parallel**.
4. Measure before and after with GC logs (`-Xlog:gc*`). Never tune GC flags by folklore — most old Java 8 tuning flags do nothing (or harm) on modern collectors.

---

## Summary

| Java 8 | Java 17 |
|---|---|
| Default: Parallel GC (throughput, long pauses) | Default: G1 (balanced, pause-time goal) |
| CMS as the "low-latency" option | CMS removed; ZGC and Shenandoah give sub-millisecond pauses |
| Full GC in G1 was single-threaded | Parallel full GC, abortable mixed collections |
| Unused heap rarely returned to the OS | G1 and metaspace return memory promptly |
| Heap tuning often needed | Sensible, container-aware defaults |
