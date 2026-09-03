package java8.defaultmethods;

public interface Flyable {

    default String move() {
        return "flying";
    }
}
