package java17.sealed;

public final class Square implements Shape {

    private final double side;

    public Square(double side) {
        this.side = side;
    }

    public double side() {
        return side;
    }
}
