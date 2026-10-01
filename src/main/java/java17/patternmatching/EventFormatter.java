package java17.patternmatching;

import java.math.BigDecimal;

import java17.patternmatching.Event.Login;
import java17.patternmatching.Event.Logout;
import java17.patternmatching.Event.Purchase;

public final class EventFormatter {

    private static final BigDecimal LARGE_PURCHASE = new BigDecimal("1000");

    private EventFormatter() {
    }

    public static String format(Event event) {
        // Record patterns deconstruct the record straight into its
        // components. Sealed Event makes the switch exhaustive without default.
        return switch (event) {
            case Login(var user) -> user + " logged in";
            case Logout(var user) -> user + " logged out";
            case Purchase(var user, var amount) when amount.compareTo(LARGE_PURCHASE) >= 0 ->
                    "LARGE purchase by " + user + ": " + amount;
            case Purchase(var user, var amount) -> user + " bought for " + amount;
        };
    }
}
