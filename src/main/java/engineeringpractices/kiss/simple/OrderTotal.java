package engineeringpractices.kiss.simple;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Just wiring: every rule lives in its own small class. The collaborators are pure
 * and fast, so there is nothing to inject or mock — a test double would be more
 * complex than the real thing.
 */
public class OrderTotal {

    private final LoyaltyDiscount loyaltyDiscount = new LoyaltyDiscount();
    private final ShippingFee shippingFee = new ShippingFee();

    public BigDecimal of(List<OrderLine> lines, CustomerTier tier) {
        var subtotal = lines.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        var discounted = loyaltyDiscount.applyTo(subtotal, tier);
        return discounted.add(shippingFee.feeFor(discounted)).setScale(2, RoundingMode.HALF_UP);
    }
}
