package java8.defaultmethods;

public interface Greeter {

    String name();

    default String greet() {
        return "Hello, " + name() + "!";
    }

    static Greeter uppercase(String name) {
        return () -> name.toUpperCase();
    }
}
