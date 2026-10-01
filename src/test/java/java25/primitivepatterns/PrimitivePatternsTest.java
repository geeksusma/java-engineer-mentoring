package java25.primitivepatterns;

import java25.primitivepatterns.PrimitivePatterns.SensorReading;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimitivePatternsTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 127, -128, 128, 32_767, 40_000, Integer.MIN_VALUE})
    void should_matchLegacyRangeChecks_when_narrowingWithPrimitiveInstanceof(int value) {
        assertEquals(LegacyNarrowing.narrowestType(value), PrimitivePatterns.narrowestType(value));
    }

    @Test
    void should_pickTheNarrowestSafeType_when_valueFits() {
        assertEquals("byte 42", PrimitivePatterns.narrowestType(42));
        assertEquals("short 1000", PrimitivePatterns.narrowestType(1000));
        assertEquals("int 100000", PrimitivePatterns.narrowestType(100_000));
    }

    @Test
    void should_matchIntOnlyWithoutPrecisionLoss_when_testingADouble() {
        assertTrue(PrimitivePatterns.isExactInt(3.0));
        assertFalse(PrimitivePatterns.isExactInt(3.5));
        assertFalse(PrimitivePatterns.isExactInt(1e20));
    }

    @Test
    void should_rejectIntsThatFloatCannotRepresentExactly_when_testingInstanceofFloat() {
        assertTrue(PrimitivePatterns.fitsExactlyInFloat(16_777_216));   // 2^24
        assertFalse(PrimitivePatterns.fitsExactlyInFloat(16_777_217));  // 2^24 + 1
        // The plain cast silently rounds instead:
        assertEquals(16_777_216f, (float) 16_777_217);
    }

    @Test
    void should_combineConstantsAndGuards_when_switchingOnAnInt() {
        assertEquals("OK", PrimitivePatterns.classifyHttpStatus(200));
        assertEquals("Success (201)", PrimitivePatterns.classifyHttpStatus(201));
        assertEquals("Not Found", PrimitivePatterns.classifyHttpStatus(404));
        assertEquals("Client error (418)", PrimitivePatterns.classifyHttpStatus(418));
        assertEquals("Server error (503)", PrimitivePatterns.classifyHttpStatus(503));
        assertEquals("Unknown (99)", PrimitivePatterns.classifyHttpStatus(99));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 512L, 2048L, 5L * 1024 * 1024})
    void should_matchLegacyIfElseChain_when_switchingOnALong(long bytes) {
        assertEquals(LegacyNarrowing.describeFileSize(bytes), PrimitivePatterns.describeFileSize(bytes));
    }

    @Test
    void should_beExhaustiveWithoutDefault_when_switchingOnABoolean() {
        assertEquals("ON", PrimitivePatterns.toggleLabel(true));
        assertEquals("OFF", PrimitivePatterns.toggleLabel(false));
    }

    @Test
    void should_matchNestedPrimitivePattern_when_recordComponentConvertsExactly() {
        assertEquals("exact reading 21", PrimitivePatterns.describe(new SensorReading(21.0)));
        assertEquals("approximate reading 21.5", PrimitivePatterns.describe(new SensorReading(21.5)));
    }
}
