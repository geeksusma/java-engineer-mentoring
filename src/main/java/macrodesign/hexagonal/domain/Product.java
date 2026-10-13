package macrodesign.hexagonal.domain;

import java.math.BigDecimal;

/**
 * Pure domain: no annotations, no persistence types, no framework. The stock
 * rule lives here instead of in a service that reads rows.
 */
public record Product(ProductCode code, BigDecimal unitPrice, int stock) {

    public Product reserve(int quantity) {
        if (quantity <= 0) {
            throw new OrderRejectedException("quantity must be positive: " + quantity);
        }
        if (stock < quantity) {
            throw new OrderRejectedException("only " + stock + " left of " + code.value());
        }
        return new Product(code, unitPrice, stock - quantity);
    }

    public BigDecimal priceFor(int quantity) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
