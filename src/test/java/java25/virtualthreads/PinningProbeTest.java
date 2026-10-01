package java25.virtualthreads;

import org.junit.jupiter.api.Test;
import support.ChildJvm;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PinningProbeTest {

    @Test
    void should_notPinTheCarrier_when_blockingInsideSynchronized() throws Exception {
        // One single carrier thread for all virtual threads.
        var result = ChildJvm.run(PinningProbe.class,
                "-Djdk.virtualThreadScheduler.parallelism=1",
                "-Djdk.virtualThreadScheduler.maxPoolSize=1");

        long elapsed = Long.parseLong(result.valueOf("elapsedMillis"));
        long serialTime = PinningProbe.TASKS * PinningProbe.LATENCY.toMillis();

        // Up to Java 23 each sleeping virtual thread pinned the only carrier,
        // so the tasks ran one after another: ~1500 ms. Since Java 24
        // (JEP 491) they unmount and all sleep at the same time: ~300 ms.
        assertTrue(elapsed < serialTime / 2, "took " + elapsed + " ms");
    }
}
