package basics.equalshashcode;

import java.util.Objects;

/**
 * Overrides both equals() and hashCode() consistently — the correct, hand-written way.
 */
public class GoodPoint {

    private final int x;
    private final int y;

    public GoodPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoodPoint other)) return false;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
