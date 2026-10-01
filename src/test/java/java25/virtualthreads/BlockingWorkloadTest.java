package java25.virtualthreads;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockingWorkloadTest {

    private static final int TASKS = 1_000;
    private static final Duration LATENCY = Duration.ofMillis(100);

    @Test
    void should_runEveryBlockingTaskConcurrently_when_usingOneVirtualThreadPerTask() {
        long start = System.nanoTime();

        int completed = BlockingWorkload.runOnVirtualThreads(TASKS, LATENCY);

        var elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertEquals(TASKS, completed);
        // 1,000 tasks x 100 ms, yet the whole batch takes roughly one latency.
        assertTrue(elapsed.compareTo(Duration.ofSeconds(1)) < 0, "took " + elapsed);
    }

    @Test
    void should_queueBehindThePoolSize_when_usingPlatformThreads() {
        long start = System.nanoTime();

        int completed = BlockingWorkload.runOnPlatformThreadPool(50, TASKS, LATENCY);

        var elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertEquals(TASKS, completed);
        // 1,000 tasks / 50 threads = 20 waves of 100 ms each.
        assertTrue(elapsed.compareTo(Duration.ofMillis(2_000)) >= 0, "took " + elapsed);
    }

    @Test
    void should_beVirtual_when_createdWithTheThreadBuilder() throws InterruptedException {
        var wasVirtual = new AtomicBoolean();

        Thread thread = Thread.ofVirtual().name("worker-", 0).start(
                () -> wasVirtual.set(Thread.currentThread().isVirtual()));
        thread.join();

        assertTrue(wasVirtual.get());
        assertEquals("worker-0", thread.getName());
        assertFalse(Thread.currentThread().isVirtual());
    }
}
