package basics.equalshashcode.dsl;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hides the HashSet plumbing behind vocabulary the lesson is actually about:
 * "is an equal element found?" — not "call add() then contains()".
 */
public final class HashSetExpectation<T> {

    private final Set<T> bucket = new HashSet<>();

    private HashSetExpectation() {
    }

    public static <T> HashSetExpectation<T> aHashSetContaining(T element) {
        HashSetExpectation<T> expectation = new HashSetExpectation<>();
        expectation.bucket.add(element);
        return expectation;
    }

    public void shouldFind(T equalElement) {
        assertTrue(bucket.contains(equalElement),
                () -> "Expected the set to contain an element equal to " + equalElement);
    }

    public void shouldNotFind(T equalElement) {
        assertFalse(bucket.contains(equalElement),
                () -> "Expected the set to NOT contain an element equal to " + equalElement
                        + " (but it does — hashCode()/equals() may be inconsistent)");
    }
}
