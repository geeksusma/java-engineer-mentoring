package engineeringpractices.kiss.simple;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoyaltyDiscountTest {

    private final LoyaltyDiscount loyaltyDiscount = new LoyaltyDiscount();

    @Test
    void should_takeTenPercentOff_when_customerIsGold() {
        assertEquals(0, new BigDecimal("90").compareTo(loyaltyDiscount.applyTo(new BigDecimal("100"), CustomerTier.GOLD)));
    }

    @Test
    void should_keepTheAmount_when_customerIsStandard() {
        assertEquals(new BigDecimal("100"), loyaltyDiscount.applyTo(new BigDecimal("100"), CustomerTier.STANDARD));
    }
}
