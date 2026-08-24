package basics.equalshashcode;

import org.junit.jupiter.api.Test;

import static basics.equalshashcode.dsl.EqualsHashCodeContract.shouldHoldFor;
import static basics.equalshashcode.dsl.HashMapExpectation.aHashMapWithEntry;
import static basics.equalshashcode.dsl.HashSetExpectation.aHashSetContaining;

/**
 * Same idea as BadPointTest, but with equals()/hashCode() overridden
 * consistently — everything works the way you'd intuitively expect.
 */
class GoodPointTest {

    @Test
    void should_holdTheHashCodeContract_when_bothMethodsAreOverridden() {
        shouldHoldFor(new GoodPoint(1, 2), new GoodPoint(1, 2));
    }

    @Test
    void should_findAnEqualObject_when_lookingItUpInAHashSet() {
        aHashSetContaining(new GoodPoint(1, 2))
                .shouldFind(new GoodPoint(1, 2));
    }

    @Test
    void should_returnTheValue_when_lookingUpWithAnEqualKeyInAHashMap() {
        aHashMapWithEntry(new GoodPoint(0, 0), "origin")
                .lookingUpShouldReturn(new GoodPoint(0, 0), "origin");
    }
}
