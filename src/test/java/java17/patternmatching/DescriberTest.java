package java17.patternmatching;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescriberTest {

    static Stream<Arguments> values() {
        return Stream.of(
                Arguments.of(null, "null"),
                Arguments.of(-5, "negative integer -5"),
                Arguments.of(42, "integer 42"),
                Arguments.of("   ", "blank string"),
                Arguments.of("hello", "string of length 5"),
                Arguments.of(new int[]{1, 2, 3}, "int array of 3"),
                Arguments.of(3.14, "unknown: Double"));
    }

    @ParameterizedTest
    @MethodSource("values")
    void should_describeTheSameWay_when_usingLegacyInstanceofOrPatternSwitch(Object value, String expected) {
        assertEquals(expected, LegacyDescriber.describe(value));
        assertEquals(expected, ModernDescriber.describe(value));
    }

    @ParameterizedTest
    @MethodSource("values")
    void should_keepBindingInScopeAfterNegatedInstanceof_when_matchSucceeded(Object value, String ignored) {
        int expected = value instanceof String text ? text.length() : -1;

        assertEquals(expected, ModernDescriber.lengthOrMinusOne(value));
    }
}
