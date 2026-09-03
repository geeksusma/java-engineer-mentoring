package java8.defaultmethods;

public interface Swimmable {

    default String move() {
        return "swimming";
    }
}
