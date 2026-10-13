package macrodesign.boundedcontext.contracts;

import java.util.List;

/**
 * Published language: the only shape both contexts agree on. Plain values, no
 * model classes from either side, versioned and changed with care because
 * other teams depend on it.
 */
public record OrderPlaced(String orderId, String deliveryAddress, List<Line> lines) {

    public OrderPlaced {
        lines = List.copyOf(lines);
    }

    public record Line(String sku, int quantity) {
    }
}
