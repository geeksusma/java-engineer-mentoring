package basics.equalshashcode;

import org.junit.jupiter.api.Test;

import static basics.equalshashcode.dsl.EqualsHashCodeContract.shouldHoldFor;
import static basics.equalshashcode.dsl.HashSetExpectation.aHashSetContaining;

/**
 * Records generate a correct equals()/hashCode() pair automatically —
 * behaves just like GoodPointTest, with none of the boilerplate.
 */
class PointRecordTest {

    @Test
    void should_holdTheHashCodeContract_when_typeIsARecord() {
        shouldHoldFor(new PointRecord(1, 2), new PointRecord(1, 2));
    }

    @Test
    void should_findAnEqualRecord_when_lookingItUpInAHashSet() {
        aHashSetContaining(new PointRecord(1, 2))
                .shouldFind(new PointRecord(1, 2));
    }
}
