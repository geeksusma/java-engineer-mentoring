package java8.optional;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * orElse(T) evaluates its argument unconditionally. orElseGet(Supplier<T>)
 * only evaluates it when the Optional is actually empty. Mixing them up is
 * the sharpest edge in the Optional API.
 */
class OptionalEagerVsLazyTest {

    @Test
    void should_evaluateTheFallback_when_usingOrElseEvenThoughTheOptionalIsPresent() {
        AtomicInteger fallbackCalls = new AtomicInteger();

        Optional.of("present").orElse(expensiveDefault(fallbackCalls));

        assertEquals(1, fallbackCalls.get(), "orElse's argument is evaluated eagerly, regardless of presence");
    }

    @Test
    void should_notEvaluateTheSupplier_when_usingOrElseGetAndTheOptionalIsPresent() {
        AtomicInteger fallbackCalls = new AtomicInteger();

        Optional.of("present").orElseGet(() -> expensiveDefault(fallbackCalls));

        assertEquals(0, fallbackCalls.get(), "orElseGet's supplier should never run when a value is already present");
    }

    @Test
    void should_evaluateTheSupplier_when_usingOrElseGetAndTheOptionalIsEmpty() {
        AtomicInteger fallbackCalls = new AtomicInteger();

        String result = Optional.<String>empty().orElseGet(() -> expensiveDefault(fallbackCalls));

        assertEquals("default", result);
        assertEquals(1, fallbackCalls.get());
    }

    private static String expensiveDefault(AtomicInteger callCounter) {
        callCounter.incrementAndGet();
        return "default";
    }
}
