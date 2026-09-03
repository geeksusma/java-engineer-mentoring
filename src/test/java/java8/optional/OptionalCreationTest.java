package java8.optional;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Optional.of() trusts you that the value isn't null and blows up immediately
 * if you're wrong. Optional.ofNullable() is the adapter for values that
 * genuinely might be null (e.g. a legacy API or Map.get()).
 */
class OptionalCreationTest {

    @Test
    void should_throwNullPointerException_when_ofIsCalledWithNull() {
        assertThrows(NullPointerException.class, () -> Optional.of(null));
    }

    @Test
    void should_returnAnEmptyOptional_when_ofNullableIsCalledWithNull() {
        assertEquals(Optional.empty(), Optional.ofNullable(null));
    }

    @Test
    void should_returnAPresentOptional_when_ofNullableIsCalledWithANonNullValue() {
        assertTrue(Optional.ofNullable("value").isPresent());
    }
}
