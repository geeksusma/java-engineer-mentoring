package engineeringpractices.kiss.complicated;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Same rules as the simple version, but look at what every test has to know:
 * magic keys, the step names, their order, and a mutable map to hold it all.
 */
class PricingEngineTest {

    private static final List<String> FULL_PIPELINE = List.of("subtotal", "loyalty", "shipping");

    @Test
    void should_addShipping_when_testingOnlyTheShippingRule() {
        // We only care about shipping, yet we need the subtotal step, a line map and a tier key.
        var context = new HashMap<String, Object>();
        context.put(PricingEngine.LINES, List.of(Map.of("price", "10.00", "qty", 1)));
        context.put(PricingEngine.TIER, "STANDARD");

        assertEquals(new BigDecimal("14.99"), new PricingEngine(FULL_PIPELINE).run(context));
    }

    @Test
    void should_giveADifferentTotal_when_stepsAreOrderedDifferently() {
        // 55.00 -> shipping is free -> GOLD 10% off = 49.50 ... but the "right" order charges 4.99.
        var context = new HashMap<String, Object>();
        context.put(PricingEngine.LINES, List.of(Map.of("price", "55.00", "qty", 1)));
        context.put(PricingEngine.TIER, "GOLD");

        var total = new PricingEngine(List.of("subtotal", "shipping", "loyalty")).run(context);

        assertEquals(new BigDecimal("49.50"), total);
    }

    @Test
    void should_failAtRuntime_when_aStepNameHasATypo() {
        var context = new HashMap<String, Object>();
        context.put(PricingEngine.LINES, List.of());

        assertThrows(IllegalArgumentException.class,
                () -> new PricingEngine(List.of("subtotal", "loyality")).run(context));
    }

    @Test
    void should_failAtRuntime_when_aKeyIsMissing() {
        assertThrows(NullPointerException.class,
                () -> new PricingEngine(List.of("subtotal")).run(new HashMap<>()));
    }
}
