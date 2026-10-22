package engineeringpractices.kiss.complicated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The "flexible" version: a configurable pipeline of named steps sharing a
 * {@code Map<String, Object>} context. Nothing here is a bug — it's just far more
 * machinery than three business rules need, and every test pays for it.
 */
public class PricingEngine {

    public static final String LINES = "lines";
    public static final String TIER = "tier";
    public static final String AMOUNT = "amount";

    private final Map<String, PricingStep> registry = new LinkedHashMap<>();
    private final List<String> pipeline;

    public PricingEngine(List<String> pipeline) {
        this.pipeline = pipeline;
        registry.put("subtotal", context -> {
            var sum = BigDecimal.ZERO;
            for (Object item : (List<?>) context.get(LINES)) {
                var line = (Map<?, ?>) item;
                sum = sum.add(new BigDecimal(line.get("price").toString())
                        .multiply(new BigDecimal(line.get("qty").toString())));
            }
            context.put(AMOUNT, sum);
        });
        registry.put("loyalty", context -> {
            if ("GOLD".equals(context.get(TIER))) {
                var amount = (BigDecimal) context.get(AMOUNT);
                context.put(AMOUNT, amount.subtract(amount.multiply(new BigDecimal("0.10"))));
            }
        });
        registry.put("shipping", context -> {
            var amount = (BigDecimal) context.get(AMOUNT);
            if (amount.compareTo(new BigDecimal("50")) < 0) {
                context.put(AMOUNT, amount.add(new BigDecimal("4.99")));
            }
        });
    }

    public BigDecimal run(Map<String, Object> context) {
        for (String name : pipeline) {
            var step = registry.get(name);
            if (step == null) {
                throw new IllegalArgumentException("Unknown pricing step: " + name);
            }
            step.apply(context);
        }
        return ((BigDecimal) context.get(AMOUNT)).setScale(2, RoundingMode.HALF_UP);
    }

    @FunctionalInterface
    public interface PricingStep {

        void apply(Map<String, Object> context);
    }
}
