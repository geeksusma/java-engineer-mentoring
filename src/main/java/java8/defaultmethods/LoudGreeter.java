package java8.defaultmethods;

/**
 * Overrides the default, but reuses it via Greeter.super.greet() instead of
 * duplicating the "Hello, X!" logic.
 */
public class LoudGreeter implements Greeter {

    private final String name;

    public LoudGreeter(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String greet() {
        return Greeter.super.greet().toUpperCase();
    }
}
