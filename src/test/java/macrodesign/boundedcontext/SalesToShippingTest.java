package macrodesign.boundedcontext;

import macrodesign.boundedcontext.contracts.OrderPlaced;
import macrodesign.boundedcontext.messaging.EventBus;
import macrodesign.boundedcontext.sales.Checkout;
import macrodesign.boundedcontext.sales.Customer;
import macrodesign.boundedcontext.shipping.Carrier;
import macrodesign.boundedcontext.shipping.Shipment;
import macrodesign.boundedcontext.shipping.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalesToShippingTest {

    // the same SKU, modelled twice, once per context
    private static final macrodesign.boundedcontext.sales.Product MONITOR_FOR_SALE =
            new macrodesign.boundedcontext.sales.Product("MON-27", "27\" monitor", new BigDecimal("199.00"));
    private static final macrodesign.boundedcontext.sales.Product PAD_FOR_SALE =
            new macrodesign.boundedcontext.sales.Product("PAD-01", "Mouse pad", new BigDecimal("9.00"));

    private static final Customer ANA = new Customer("C-1", "Ana", "Gran Via 1, Madrid", true);

    private final EventBus bus = new EventBus();
    private final Warehouse warehouse = new Warehouse();
    private final Checkout checkout = new Checkout(bus);

    @BeforeEach
    void setUp() {
        warehouse.stock(new macrodesign.boundedcontext.shipping.Product("MON-27", 6_500, true));
        warehouse.stock(new macrodesign.boundedcontext.shipping.Product("PAD-01", 150, false));
        bus.subscribe(OrderPlaced.class, warehouse::on);
    }

    @Test
    void should_applySalesRules_when_placingAnOrder() {
        var order = checkout.placeOrder(ANA, Map.of(MONITOR_FOR_SALE, 1, PAD_FOR_SALE, 2));

        assertEquals(new BigDecimal("195.30"), order.total()); // 217.00 minus 10% loyalty discount
    }

    @Test
    void should_applyShippingRules_when_theOrderPlacedEventArrives() {
        checkout.placeOrder(ANA, Map.of(MONITOR_FOR_SALE, 1, PAD_FOR_SALE, 2));

        assertEquals(List.of(new Shipment("SO-1", "Gran Via 1, Madrid", 6_800, true, Carrier.COURIER)),
                warehouse.shipments());
    }

    @Test
    void should_chooseFreight_when_shipmentIsVeryHeavy() {
        checkout.placeOrder(ANA, Map.of(MONITOR_FOR_SALE, 5));

        assertEquals(Carrier.FREIGHT, warehouse.shipments().getFirst().carrier());
    }
}
