package java17.sealed;

/**
 * non-sealed re-opens the hierarchy from this point down: anyone can extend
 * Polygon, but they are still Shapes, so the set of direct Shape subtypes
 * stays closed.
 */
public abstract non-sealed class Polygon implements Shape {

    public abstract double area();
}
