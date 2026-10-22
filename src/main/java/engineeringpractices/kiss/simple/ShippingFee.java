package engineeringpractices.kiss.simple;

import java.math.BigDecimal;

public class ShippingFee {

    private static final BigDecimal FREE_SHIPPING_FROM = new BigDecimal("50");
    private static final BigDecimal STANDARD_FEE = new BigDecimal("4.99");

    public BigDecimal feeFor(BigDecimal amount) {
        return amount.compareTo(FREE_SHIPPING_FROM) >= 0 ? BigDecimal.ZERO : STANDARD_FEE;
    }
}
