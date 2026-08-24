package basics.equalshashcode.dsl;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Hides HashMap plumbing behind "given these entries, looking up this key
 * returns this value" — the actual behavior the lesson demonstrates.
 */
public final class HashMapExpectation<K, V> {

    private final Map<K, V> map = new HashMap<>();

    private HashMapExpectation() {
    }

    public static <K, V> HashMapExpectation<K, V> aHashMapWithEntry(K key, V value) {
        return new HashMapExpectation<K, V>().andEntry(key, value);
    }

    public HashMapExpectation<K, V> andEntry(K key, V value) {
        map.put(key, value);
        return this;
    }

    public void lookingUpShouldReturn(K key, V expectedValue) {
        assertEquals(expectedValue, map.get(key),
                "Expected looking up a key equal to " + key + " to return " + expectedValue);
    }

    public void sizeShouldBe(int expectedSize) {
        assertEquals(expectedSize, map.size());
    }
}
