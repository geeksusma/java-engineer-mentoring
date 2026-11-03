# Virtual Threads and Concurrent improvements

Java 21 finalized **virtual threads** (JEP 444), and the releases up to the Java 25 LTS polished the story around them: `synchronized` no longer pins them (Java 24, JEP 491), **Scoped Values** are final (Java 25, JEP 506), and **Structured Concurrency** is in its fifth preview (Java 25, JEP 505). Together they bring back the simplest concurrency model there is — *one thread per task, just block* — at a scale that used to require reactive frameworks.

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (var request : requests) {
        executor.submit(() -> handle(request));   // one cheap virtual thread per task
    }
}
```

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java25/virtualthreads`](../../../src/test/java/java25/virtualthreads) (classes under `src/main/java/java25/virtualthreads`). Run them with `mvn test`. `StructuredTaskScope` is a preview API, so this project compiles and runs with `--enable-preview` (see `pom.xml`).

---

## The problem: platform threads are expensive

Until Java 21, every `java.lang.Thread` was a thin wrapper around an **operating-system thread** (now called a *platform thread*). OS threads are heavyweight — around 1 MB of reserved stack each, and costly to create and context-switch — so a JVM realistically runs a few thousand of them.

Typical backend code spends most of its time **waiting**: on a database, an HTTP call, a queue. In the classic thread-per-request model, a thread blocked on I/O does nothing but still holds its OS thread. So the thread pool size becomes the ceiling on how many requests you can serve concurrently.

The industry's answer was reactive/asynchronous programming (`CompletableFuture` chains, WebFlux, RxJava): never block, so a handful of threads can juggle thousands of requests. It works — but code becomes callback chains, stack traces become useless, and debugging gets painful.

---

## Virtual threads: blocking becomes cheap

A **virtual thread** is a `Thread` managed by the JVM, not the OS. The JVM runs many virtual threads on a small pool of platform threads called **carriers** (a `ForkJoinPool` sized to the CPU count by default):

- When a virtual thread **blocks** (sleep, socket read, lock, `BlockingQueue.take()`...), the JVM **unmounts** it: its stack is saved on the heap and the carrier is freed to run another virtual thread.
- When the blocking operation completes, the virtual thread is **mounted** again on any free carrier and continues.

Your code still looks sequential and blocking. The JVM does the juggling that reactive code did by hand.

### Proof: 1,000 blocking tasks

Each task sleeps 100 ms (standing in for any blocking call):

```java
int completed = BlockingWorkload.runOnVirtualThreads(1_000, Duration.ofMillis(100));
// All 1,000 run at the same time: the whole batch takes ~100-200 ms.
```

```java
int completed = BlockingWorkload.runOnPlatformThreadPool(50, 1_000, Duration.ofMillis(100));
// 1,000 tasks / 50 threads = 20 waves of 100 ms: at least 2 seconds.
```

Same code, same task — the only difference is the executor. Scale it to 100,000 tasks and the virtual version still finishes in well under a second; the platform version would need 100,000 OS threads or a very long queue.

### Creating virtual threads

```java
Executors.newVirtualThreadPerTaskExecutor();              // one new virtual thread per task
Thread.ofVirtual().name("worker-", 0).start(runnable);    // builder API
Thread.startVirtualThread(runnable);                      // shortcut
Thread.currentThread().isVirtual();                       // check at runtime
```

In Spring Boot 3.2+, a single property moves request handling onto virtual threads: `spring.threads.virtual.enabled=true`.

---

## Rules of thumb

- **Don't pool virtual threads.** They're cheap and meant to be created per task and thrown away. A pool of virtual threads is a pool of nothing. If you need to **limit concurrency** (e.g. max 10 calls to a fragile API), use a `Semaphore`, not a small pool.
- **They help I/O-bound work, not CPU-bound work.** A virtual thread doesn't make computation faster; a CPU-bound task never blocks, so it never unmounts. For number crunching, keep using parallel streams or a `ForkJoinPool`.
- **Be careful with `ThreadLocal`.** It works, but with millions of threads, per-thread caches (the classic "one `SimpleDateFormat` per thread" trick) multiply memory use. Prefer Scoped Values (below) for context passing.

---

## Java 24: `synchronized` no longer pins (JEP 491)

In Java 21–23, blocking **inside a `synchronized` block** *pinned* the virtual thread to its carrier: it couldn't unmount, so the carrier was stuck too. With lots of legacy `synchronized` code (JDBC drivers, older libraries), this could starve the carrier pool, and the standard advice was "replace `synchronized` with `ReentrantLock`".

Java 24 reimplemented object monitors so virtual threads can unmount while holding or waiting for one. `PinningProbe` proves it by running 5 virtual threads on **a single carrier**, each sleeping 300 ms inside a `synchronized` method:

```java
public synchronized void slowDeposit(long amount, Duration latency) throws InterruptedException {
    Thread.sleep(latency);
    balance += amount;
}
```

```
java -Djdk.virtualThreadScheduler.parallelism=1 \
     -Djdk.virtualThreadScheduler.maxPoolSize=1 ... PinningProbe
elapsedMillis=308
```

- On Java 21–23: the only carrier is pinned by each sleeping thread in turn → **~1,500 ms**.
- On Java 24+: each thread unmounts while sleeping → all five sleep at once → **~300 ms**.

Pinning still happens in rare cases (e.g. blocking inside native code called through JNI), but `synchronized` is no longer a reason to rewrite code.

---

## Scoped Values: the successor to `ThreadLocal` (final in Java 25)

`ThreadLocal` is how frameworks pass implicit context (request id, user, transaction) down the call stack. It has problems: it's mutable (anyone can `set()`), it lives as long as the thread unless someone remembers `remove()` — a classic leak with pools — and copying it into child threads is expensive.

A `ScopedValue` is **immutable** and **bound for a bounded block of code**:

```java
public static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

public static String handle(String requestId) {
    return ScopedValue.where(REQUEST_ID, requestId).call(RequestContext::process);
}

static String process() {
    return "processed request " + REQUEST_ID.get();   // no parameter threading
}
```

- Inside the `call`/`run` block, `REQUEST_ID.get()` returns the bound value anywhere in the call stack.
- Outside it, the value is **unbound**: `isBound()` is `false`, `get()` throws `NoSuchElementException`, `orElse(fallback)` returns the fallback.
- Rebinding in a nested scope shadows the outer value, and the outer value is automatically restored when the inner block ends — there's no `remove()` to forget.
- Values are **inherited by subtasks** forked in a `StructuredTaskScope` (see the test `should_inheritScopedValues_when_forkingSubtasks`).

---

## Structured Concurrency (preview in Java 25)

With plain executors, the subtasks of an operation are unrelated threads: if one fails, the others keep running and wasting resources, and nothing ties their lifetime to the caller's. **Structured concurrency** treats a group of subtasks as one unit of work, scoped to a block of code:

```java
public static Dashboard load(Callable<String> fetchUser, Callable<List<String>> fetchOrders)
        throws InterruptedException {
    try (var scope = StructuredTaskScope.open()) {
        Subtask<String> user = scope.fork(fetchUser);
        Subtask<List<String>> orders = scope.fork(fetchOrders);

        scope.join();

        return new Dashboard(user.get(), orders.get());
    }
}
```

- Each `fork` runs in its own virtual thread, so both calls happen **concurrently** (two 300 ms calls take ~300 ms in total).
- `join()` waits for the subtasks. With the default policy of `open()`, **if any subtask fails, the others are cancelled** (interrupted) and `join()` throws `StructuredTaskScope.FailedException`, whose cause is the original exception.
- The `try`-with-resources guarantees no subtask outlives the block: `close()` waits for all of them to finish.

The test proves the fail-fast behaviour: when the orders call throws immediately, the 10-second user call is interrupted and the whole thing fails in milliseconds instead of waiting 10 seconds.

Other policies are available through `StructuredTaskScope.Joiner` (for example "first successful result wins"). Since it's still a preview API, expect small changes before it's final.

---

## Summary

| Feature | Status in Java 25 | What it gives you |
|---|---|---|
| Virtual threads | Final since 21 (JEP 444) | Millions of cheap threads; blocking code scales like async code |
| `synchronized` without pinning | Since 24 (JEP 491) | Legacy `synchronized` code is safe on virtual threads |
| Scoped Values | Final in 25 (JEP 506) | Immutable, bounded, inheritable context — replaces most `ThreadLocal` use |
| Structured Concurrency | Preview in 25 (JEP 505) | Subtasks as one unit: fail fast, cancel siblings, no leaked threads |

| Platform threads | Virtual threads |
|---|---|
| 1:1 with OS threads, ~thousands per JVM | Managed by the JVM, millions per JVM |
| Expensive: pool and reuse them | Cheap: create one per task, never pool |
| Blocking wastes an OS thread | Blocking unmounts, the carrier moves on |
| Scalability required async/reactive code | Plain sequential blocking code scales |
