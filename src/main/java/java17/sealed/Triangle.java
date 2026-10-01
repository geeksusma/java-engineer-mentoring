package java17.sealed;

/**
 * Not listed in Shape's permits clause — allowed only because Polygon is
 * non-sealed.
 */
public class Triangle extends Polygon {

    private final double base;
    private final double height;

    public Triangle(double base, double height) {
        this.base = base;
        this.height = height;
    }

    @Override
    public double area() {
        return base * height / 2;
    }
}
