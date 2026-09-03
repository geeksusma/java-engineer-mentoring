# Default Methods in Interfaces

Before Java 8, an interface could only declare method signatures — every method was implicitly `public abstract`, and adding a new method to an interface broke every existing class that implemented it (the class would no longer compile until it added the new method). `default` methods let an interface provide a **body** for a method, which implementing classes inherit automatically and may optionally override.

```java
public interface Greeter {
    String name();

    default String greet() {
        return "Hello, " + name() + "!";
    }
}
```

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java8/defaultmethods`](../../../src/test/java/java8/defaultmethods) (classes under `src/main/java/java8/defaultmethods`). Run them with `mvn test`.

---

## Why they were introduced: backward compatibility

This feature exists almost entirely because of the Java 8 collections overhaul. Adding `stream()`, `forEach()`, and `sort()` to `Collection`/`List` the old way would have broken **every single class in the world** that implemented those interfaces without those methods. Default methods let the JDK add new behavior to existing interfaces without forcing anyone to update their implementations:

```java
public class SimpleGreeter implements Greeter {
    private final String name;

    public SimpleGreeter(String name) {
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }
    // greet() is inherited from Greeter for free — never had to be written here
}
```

An implementing class can still override the default when it wants different behavior, and can call back into the interface's own implementation with `InterfaceName.super.method()`:

```java
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
        return Greeter.super.greet().toUpperCase(); // reuse the default, then adapt it
    }
}
```

---

## The diamond problem

Default methods reintroduce a flavor of the classic multiple-inheritance "diamond problem": what happens when a class implements two interfaces that each provide a **default method with the same signature**?

```java
public interface Flyable {
    default String move() {
        return "flying";
    }
}

public interface Swimmable {
    default String move() {
        return "swimming";
    }
}

public class FlyingFish implements Flyable, Swimmable {
    // won't compile without this — the compiler refuses to guess which default you meant
}
```

Java doesn't pick one arbitrarily and doesn't let you leave it ambiguous — `FlyingFish` **fails to compile** until you explicitly override `move()` and say what should happen, optionally delegating to one or both parents by name:

```java
public class FlyingFish implements Flyable, Swimmable {
    @Override
    public String move() {
        return Flyable.super.move() + " and " + Swimmable.super.move();
    }
}
```

This is a compile-time error, not a runtime surprise — the ambiguity is caught before the code ever ships, which is the main reason this feature is safe to have at all.

---

## Resolution rules

When multiple defaults could apply, Java resolves them in a fixed order — you only hit the diamond problem (a compile error) when none of these rules settle it:

1. **A class (or superclass) implementation always wins** over any interface default, no matter how many interfaces provide one.
2. **The most specific interface wins.** If interface `B extends A` and both declare a default for the same method, `B`'s version is used — it's assumed to be the more refined one.
3. **Otherwise, it's ambiguous** and you must override the method yourself, as with `FlyingFish` above.

---

## Static methods in interfaces

Java 8 also allowed `static` methods on interfaces — utility methods that belong conceptually to the interface but aren't inherited by (or callable on) implementing instances. `Comparator.comparing(...)` and `List.of(...)` are exactly this: called on the interface type itself, not on an instance.

```java
public interface Greeter {
    static Greeter uppercase(String name) {
        return () -> name.toUpperCase(); // Greeter as a functional interface, greet() overridden via lambda
    }
}
```

---

## Summary

| | `abstract` method | `default` method | `static` method |
|---|---|---|---|
| Has a body in the interface? | No | Yes | Yes |
| Inherited by implementing classes? | Must be implemented | Yes, unless overridden | No — called on the interface itself |
| Can cause a diamond conflict? | No (no body to conflict) | Yes, if two interfaces both provide one | No |
| Typical use | Defining the contract | Adding behavior without breaking existing implementers | Interface-scoped utility/factory methods |
