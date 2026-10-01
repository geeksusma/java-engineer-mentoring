package java25.virtualthreads;

import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Meant to run in its own JVM with a single carrier thread
 * (-Djdk.virtualThreadScheduler.parallelism=1). Prints how long it took for
 * N virtual threads to each block inside a synchronized method.
 */
public final class PinningProbe {

    public static final int TASKS = 5;
    public static final Duration LATENCY = Duration.ofMillis(300);

    private PinningProbe() {
    }

    public static void main(String[] args) {
        long start = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < TASKS; i++) {
                var account = new LegacyAccount();
                executor.submit(() -> {
                    account.slowDeposit(10, LATENCY);
                    return account.balance();
                });
            }
        }
        System.out.println("elapsedMillis=" + Duration.ofNanos(System.nanoTime() - start).toMillis());
    }
}
