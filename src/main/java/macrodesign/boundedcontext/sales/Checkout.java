package macrodesign.boundedcontext.sales;

import macrodesign.boundedcontext.contracts.OrderPlaced;
import macrodesign.boundedcontext.messaging.EventBus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Map;

/**
 * Sales' rules: pricing and loyalty discounts. When it's done, it announces
 * the fact and forgets about it — shipping is somebody else's problem.
 */
public class Checkout {

    private static final BigDecimal LOYALTY_DISCOUNT = new BigDecimal("0.90");

    private final EventBus bus;
    private int sequence = 0;

    public Checkout(EventBus bus) {
        this.bus = bus;
    }

    public Order placeOrder(Customer customer, Map<Product, Integer> lines) {
        var total = lines.entrySet().stream()
                .map(line -> line.getKey().price().multiply(BigDecimal.valueOf(line.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (customer.loyaltyMember()) {
            total = total.multiply(LOYALTY_DISCOUNT).setScale(2, RoundingMode.HALF_EVEN);
        }
        var order = new Order("SO-" + (++sequence), customer, lines, total);

        bus.publish(new OrderPlaced(order.id(), customer.address(), lines.entrySet().stream()
                .map(line -> new OrderPlaced.Line(line.getKey().sku(), line.getValue()))
                .sorted(Comparator.comparing(OrderPlaced.Line::sku))
                .toList()));
        return order;
    }
}
