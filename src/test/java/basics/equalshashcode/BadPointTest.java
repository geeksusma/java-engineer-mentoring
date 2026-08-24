package basics.equalshashcode;

import org.junit.jupiter.api.Test;

import static basics.equalshashcode.dsl.EqualsHashCodeContract.shouldBeViolatedFor;
import static basics.equalshashcode.dsl.HashSetExpectation.aHashSetContaining;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demonstrates the bug you get from overriding equals() without hashCode():
 * two objects that ARE equal end up in different HashSet buckets, so
 * lookups that should succeed silently fail.
 */
class BadPointTest {

    @Test
    void should_beEqual_when_coordinatesMatch() {
        assertEquals(new BadPoint(1, 2), new BadPoint(1, 2));
    }

    @Test
    void should_violateTheHashCodeContract_when_hashCodeIsNotOverridden() {
        // a.equals(b) is true, so a.hashCode() == b.hashCode() MUST also be
        // true. It isn't here, because hashCode() was never overridden.
        shouldBeViolatedFor(new BadPoint(1, 2), new BadPoint(1, 2));
    }

    @Test
    void should_notFindAnEqualObject_when_lookingItUpInAHashSet() {
        // We'd expect this to be found — it's logically the same point —
        // but the broken hashCode() sends it to a different bucket, so
        // equals() is never even called to compare it.
        aHashSetContaining(new BadPoint(1, 2))
                .shouldNotFind(new BadPoint(1, 2));
    }
}
