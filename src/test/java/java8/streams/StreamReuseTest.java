package java8.streams;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A Stream models a single walk over its source. Once a terminal operation
 * has consumed it, it's spent — reusing it is a programming error, not a
 * silent no-op.
 */
class StreamReuseTest {

    @Test
    void should_throwIllegalStateException_when_aTerminalOperationRunsTwiceOnTheSameStream() {
        Stream<String> stream = List.of("a", "b", "c").stream();

        stream.toList();

        assertThrows(IllegalStateException.class, stream::toList);
    }
}
