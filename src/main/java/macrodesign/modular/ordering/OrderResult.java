package macrodesign.modular.ordering;

import java.math.BigDecimal;

public sealed interface OrderResult {

    record Placed(BigDecimal total) implements OrderResult {
    }

    record Rejected(String reason) implements OrderResult {
    }

    /**
     * Never happens while the catalog runs in the same process — it is the
     * price of a network hop, and it belongs in the API from day one.
     */
    record RetryLater(String reason) implements OrderResult {
    }
}
