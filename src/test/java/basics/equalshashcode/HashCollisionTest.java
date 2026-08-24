package basics.equalshashcode;

import org.junit.jupiter.api.Test;

import static basics.equalshashcode.dsl.EqualsHashCodeContract.shouldCollideWithoutBeingEqualFor;
import static basics.equalshashcode.dsl.HashMapExpectation.aHashMapWithEntry;

/**
 * Proves the half of the contract that trips people up most: sharing a
 * hashCode does NOT mean two objects are equal. Collisions are normal,
 * and HashMap still resolves them correctly via equals().
 */
class HashCollisionTest {

    @Test
    void should_collideOnHashCode_when_stringsAreDifferentButHashToTheSameValue() {
        // Classic textbook collision.
        shouldCollideWithoutBeingEqualFor("Aa", "BB");
    }

    @Test
    void should_collideOnHashCode_when_keysAreDeliberatelyDesignedToCollide() {
        shouldCollideWithoutBeingEqualFor(new CollidingKey(1), new CollidingKey(2));
        shouldCollideWithoutBeingEqualFor(new CollidingKey(2), new CollidingKey(3));
    }

    @Test
    void should_returnTheCorrectValue_when_keysShareABucketDueToACollision() {
        // All three CollidingKeys share a bucket (same hashCode), so
        // HashMap must fall back to equals() to tell them apart.
        var collidingEntries = aHashMapWithEntry(new CollidingKey(1), "one")
                .andEntry(new CollidingKey(2), "two")
                .andEntry(new CollidingKey(3), "three");

        collidingEntries.lookingUpShouldReturn(new CollidingKey(1), "one");
        collidingEntries.lookingUpShouldReturn(new CollidingKey(2), "two");
        collidingEntries.lookingUpShouldReturn(new CollidingKey(3), "three");
        collidingEntries.sizeShouldBe(3);
    }
}
