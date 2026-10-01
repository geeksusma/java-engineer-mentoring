package java25.virtualthreads;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardLoaderTest {

    @Test
    void should_combineBothResults_when_everySubtaskSucceeds() throws InterruptedException {
        var dashboard = DashboardLoader.load(() -> "ana", () -> List.of("o-1", "o-2"));

        assertEquals(new Dashboard("ana", List.of("o-1", "o-2")), dashboard);
    }

    @Test
    void should_runSubtasksConcurrently_when_forkedInTheSameScope() throws InterruptedException {
        long start = System.nanoTime();

        DashboardLoader.load(
                () -> sleepAndReturn(Duration.ofMillis(300), "ana"),
                () -> sleepAndReturn(Duration.ofMillis(300), List.of("o-1")));

        var elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertTrue(elapsed.compareTo(Duration.ofMillis(550)) < 0, "took " + elapsed);
    }

    @Test
    void should_cancelSiblingAndFailFast_when_oneSubtaskFails() {
        var slowUserWasInterrupted = new AtomicBoolean();
        long start = System.nanoTime();

        var failure = assertThrows(StructuredTaskScope.FailedException.class, () -> DashboardLoader.load(
                () -> {
                    try {
                        return sleepAndReturn(Duration.ofSeconds(10), "ana");
                    } catch (InterruptedException e) {
                        slowUserWasInterrupted.set(true);
                        throw e;
                    }
                },
                () -> {
                    throw new IllegalStateException("orders service is down");
                }));

        var elapsed = Duration.ofNanos(System.nanoTime() - start);
        assertInstanceOf(IllegalStateException.class, failure.getCause());
        // The scope's close() waits for the cancelled sibling to finish, so
        // by now the flag is guaranteed to be set.
        assertTrue(slowUserWasInterrupted.get());
        assertTrue(elapsed.compareTo(Duration.ofSeconds(2)) < 0, "took " + elapsed);
    }

    @Test
    void should_inheritScopedValues_when_forkingSubtasks() throws InterruptedException {
        var dashboard = ScopedValue.where(RequestContext.REQUEST_ID, "req-7").call(() ->
                DashboardLoader.load(
                        () -> "user for " + RequestContext.REQUEST_ID.get(),
                        () -> List.of("orders for " + RequestContext.REQUEST_ID.get())));

        assertEquals(new Dashboard("user for req-7", List.of("orders for req-7")), dashboard);
    }

    private static <T> T sleepAndReturn(Duration duration, T value) throws InterruptedException {
        Thread.sleep(duration);
        return value;
    }
}
