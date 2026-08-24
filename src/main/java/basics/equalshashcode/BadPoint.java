package basics.equalshashcode;

/**
 * Overrides equals() but NOT hashCode() — breaks the contract on purpose.
 * See {@link basics.equalshashcode.BadPointTest} for the resulting bug.
 */
public class BadPoint {

    private final int x;
    private final int y;

    public BadPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BadPoint other)) return false;
        return x == other.x && y == other.y;
    }

    // hashCode() intentionally NOT overridden — stays identity-based (Object's default)
}
