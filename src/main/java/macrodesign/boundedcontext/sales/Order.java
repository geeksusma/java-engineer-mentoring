package macrodesign.boundedcontext.sales;

import java.math.BigDecimal;
import java.util.Map;

public record Order(String id, Customer customer, Map<Product, Integer> lines, BigDecimal total) {

    public Order {
        lines = Map.copyOf(lines);
    }
}
