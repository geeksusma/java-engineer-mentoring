package java8.defaultmethods;

/**
 * Never implements greet() — it's inherited from Greeter's default for free,
 * exactly the backward-compatibility scenario default methods exist for.
 */
public class SimpleGreeter implements Greeter {

    private final String name;

    public SimpleGreeter(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }
}
