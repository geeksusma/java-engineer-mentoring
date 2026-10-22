package engineeringpractices.kiss.simple;

import java.math.BigDecimal;

public record OrderLine(BigDecimal unitPrice, int quantity) {

    public static OrderLine of(String unitPrice, int quantity) {
        return new OrderLine(new BigDecimal(unitPrice), quantity);
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
