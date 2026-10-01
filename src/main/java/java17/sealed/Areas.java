package java17.sealed;

public final class Areas {

    private Areas() {
    }

    public static double of(Shape shape) {
        // No default branch: the compiler knows Shape has exactly three
        // direct subtypes, so this switch is proven exhaustive. Add a fourth
        // permitted subtype and this line stops compiling until it's handled.
        return switch (shape) {
            case Circle c -> Math.PI * c.radius() * c.radius();
            case Square s -> s.side() * s.side();
            case Polygon p -> p.area();
        };
    }
}
