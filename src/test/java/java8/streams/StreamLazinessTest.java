package java8.streams;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves that intermediate operations (map, filter, ...) don't run a single
 * element until a terminal operation is invoked on the pipeline.
 */
class StreamLazinessTest {

    private final List<String> names = List.of("Ana", "Bob", "Carl", "Dana");

    @Test
    void should_notInvokeTheMapper_when_noTerminalOperationHasRunYet() {
        AtomicInteger mapCalls = new AtomicInteger();

        Stream<String> pipeline = names.stream()
                .map(n -> {
                    mapCalls.incrementAndGet();
                    return n.toUpperCase();
                });

        assertEquals(0, mapCalls.get());

        pipeline.toList();

        assertEquals(names.size(), mapCalls.get());
    }

    @Test
    void should_runEachElementThroughTheWholePipelineBeforeTheNext_when_chainingMultipleStages() {
        // If intermediate stages ran to completion one at a time (map on all
        // elements, THEN filter on all elements) the visit order recorded
        // below would be grouped by stage. Because streams are lazy and
        // element-at-a-time, it's interleaved instead: map -> filter -> map -> filter...
        List<String> visitOrder = new java.util.ArrayList<>();

        names.stream()
                .peek(n -> visitOrder.add("map:" + n))
                .map(String::toUpperCase)
                .peek(n -> visitOrder.add("filter:" + n))
                .filter(n -> n.length() <= 3)
                .toList();

        assertEquals(
                List.of("map:Ana", "filter:ANA", "map:Bob", "filter:BOB",
                        "map:Carl", "filter:CARL", "map:Dana", "filter:DANA"),
                visitOrder
        );
    }
}
