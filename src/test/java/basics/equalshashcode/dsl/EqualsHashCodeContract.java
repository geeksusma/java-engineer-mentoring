package basics.equalshashcode.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Expresses the equals()/hashCode() contract as vocabulary instead of raw
 * JUnit assertions, so a test reads as a statement about the contract
 * ("holds" / "is violated" / "collides") rather than a list of assertEquals calls.
 */
public final class EqualsHashCodeContract {

    private EqualsHashCodeContract() {
    }

    /** Objects are equal AND share a hashCode — the contract is respected. */
    public static void shouldHoldFor(Object a, Object b) {
        assertEquals(a, b, () -> a + " and " + b + " should be equal");
        assertEquals(a.hashCode(), b.hashCode(),
                () -> "Equal objects must share a hashCode, but " + a + " and " + b + " don't");
    }

    /** Objects are equal but DON'T share a hashCode — a broken override. */
    public static void shouldBeViolatedFor(Object a, Object b) {
        assertEquals(a, b, () -> a + " and " + b + " should still be equal");
        assertNotEquals(a.hashCode(), b.hashCode(),
                () -> a + " and " + b + " unexpectedly share a hashCode");
    }

    /** Objects share a hashCode but are NOT equal — an ordinary, harmless collision. */
    public static void shouldCollideWithoutBeingEqualFor(Object a, Object b) {
        assertEquals(a.hashCode(), b.hashCode(),
                () -> a + " and " + b + " were expected to collide on hashCode");
        assertNotEquals(a, b, () -> a + " and " + b + " should not be equal");
    }
}
