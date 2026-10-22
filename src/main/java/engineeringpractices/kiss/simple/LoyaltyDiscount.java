package engineeringpractices.kiss.simple;

import java.math.BigDecimal;

public class LoyaltyDiscount {

    private static final BigDecimal GOLD_RATE = new BigDecimal("0.90");

    public BigDecimal applyTo(BigDecimal amount, CustomerTier tier) {
        return tier == CustomerTier.GOLD ? amount.multiply(GOLD_RATE) : amount;
    }
}
