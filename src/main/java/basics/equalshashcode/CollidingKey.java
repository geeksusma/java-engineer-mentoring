package basics.equalshashcode;

import java.util.Objects;

/**
 * Deliberately returns the same hashCode for every instance to force hash
 * collisions on purpose, while equals() still correctly distinguishes
 * different ids. Used to prove that colliding hash codes do NOT break
 * correctness (HashMap still finds the right entry via equals()) — they
 * only degrade performance to O(n) within the shared bucket.
 */
public class CollidingKey {

    private final int id;

    public CollidingKey(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CollidingKey other)) return false;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return 42; // constant on purpose — every instance lands in the same bucket
    }

    @Override
    public String toString() {
        return "CollidingKey{id=" + id + '}';
    }
}
