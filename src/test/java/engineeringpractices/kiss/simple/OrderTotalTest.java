package engineeringpractices.kiss.simple;

import engineeringpractices.kiss.complicated.PricingEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The rules are already covered by their own tiny tests, so here we only check
 * that the pieces are wired in the right order.
 */
class OrderTotalTest {

    private final OrderTotal orderTotal = new OrderTotal();

    @Test
    void should_decideShippingOnTheDiscountedAmount_when_customerIsGold() {
        // 55.00 - 10% = 49.50, which is below 50, so shipping is charged.
        var total = orderTotal.of(List.of(OrderLine.of("55.00", 1)), CustomerTier.GOLD);

        assertEquals(new BigDecimal("54.49"), total);
    }

    @Test
    void should_addUpEveryLine_when_orderHasSeveralLines() {
        var total = orderTotal.of(List.of(OrderLine.of("20.00", 2), OrderLine.of("15.00", 1)), CustomerTier.STANDARD);

        assertEquals(new BigDecimal("55.00"), total);
    }

    @ParameterizedTest
    @CsvSource({
            "10.00, 1, STANDARD",
            "10.00, 1, GOLD",
            "55.00, 1, GOLD",
            "60.00, 1, GOLD",
            "12.34, 7, STANDARD"
    })
    void should_matchTheComplicatedEngine_when_givenTheSameOrder(String price, int quantity, CustomerTier tier) {
        var context = new HashMap<String, Object>();
        context.put(PricingEngine.LINES, List.of(Map.of("price", price, "qty", quantity)));
        context.put(PricingEngine.TIER, tier.name());
        var complicated = new PricingEngine(List.of("subtotal", "loyalty", "shipping")).run(context);

        assertEquals(complicated, orderTotal.of(List.of(OrderLine.of(price, quantity)), tier));
    }
}
