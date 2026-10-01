package java25.virtualthreads;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class BlockingWorkload {

    private BlockingWorkload() {
    }

    public static int runOnVirtualThreads(int tasks, Duration blockFor) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            return run(executor, tasks, blockFor);
        }
    }

    public static int runOnPlatformThreadPool(int poolSize, int tasks, Duration blockFor) {
        try (var executor = Executors.newFixedThreadPool(poolSize)) {
            return run(executor, tasks, blockFor);
        }
    }

    private static int run(ExecutorService executor, int tasks, Duration blockFor) {
        var completed = new AtomicInteger();
        for (int i = 0; i < tasks; i++) {
            executor.submit(() -> {
                // Stands in for any blocking call: JDBC query, HTTP request...
                Thread.sleep(blockFor);
                return completed.incrementAndGet();
            });
        }
        // ExecutorService is AutoCloseable since Java 19: close() waits for
        // every submitted task, so by the time the caller's try block exits
        // all tasks are done.
        executor.close();
        return completed.get();
    }
}
