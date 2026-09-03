package java8.streams;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves that findFirst/anyMatch/limit stop pulling elements through the
 * pipeline as soon as they have their answer, instead of visiting every
 * element like a plain terminal operation (e.g. count()) would.
 */
class StreamShortCircuitTest {

    private final List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    @Test
    void should_stopVisitingElements_when_findFirstAlreadyFoundAMatch() {
        AtomicInteger visited = new AtomicInteger();

        Optional<Integer> firstEven = numbers.stream()
                .peek(n -> visited.incrementAndGet())
                .filter(n -> n % 2 == 0)
                .findFirst();

        assertEquals(Optional.of(2), firstEven);
        assertEquals(2, visited.get(), "should have stopped right after finding 2, not scanned the whole list");
    }

    @Test
    void should_stopVisitingElements_when_anyMatchAlreadyFoundATruePredicate() {
        AtomicInteger visited = new AtomicInteger();

        boolean hasNumberAboveThree = numbers.stream()
                .peek(n -> visited.incrementAndGet())
                .anyMatch(n -> n > 3);

        assertTrue(hasNumberAboveThree);
        assertEquals(4, visited.get());
    }

    @Test
    void should_terminateAnInfiniteStream_when_limitCutsItOff() {
        List<Integer> firstFivePowersOfTwo = Stream.iterate(1, n -> n * 2)
                .limit(5)
                .toList();

        assertEquals(List.of(1, 2, 4, 8, 16), firstFivePowersOfTwo);
    }
}
