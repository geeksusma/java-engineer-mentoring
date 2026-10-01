package java17.patternmatching;

import java.math.BigDecimal;

/**
 * Permitted subtypes declared in the same file can omit the permits clause:
 * the compiler infers it.
 */
public sealed interface Event {

    record Login(String user) implements Event {
    }

    record Logout(String user) implements Event {
    }

    record Purchase(String user, BigDecimal amount) implements Event {
    }
}
