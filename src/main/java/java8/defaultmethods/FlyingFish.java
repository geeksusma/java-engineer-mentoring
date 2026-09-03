package java8.defaultmethods;

/**
 * Implements two interfaces that each provide a default move() — the classic
 * diamond problem. The compiler refuses to guess which one was meant, so
 * this override (and the explicit InterfaceName.super calls) is mandatory,
 * not stylistic.
 */
public class FlyingFish implements Flyable, Swimmable {

    @Override
    public String move() {
        return Flyable.super.move() + " and " + Swimmable.super.move();
    }
}
