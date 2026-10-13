package macrodesign.boundedcontext.shipping;

import macrodesign.boundedcontext.contracts.OrderPlaced;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shipping's own model and rules. It listens to the published event and
 * translates it into its own language at the edge of the context.
 */
public class Warehouse {

    private static final int POSTAL_LIMIT_GRAMS = 2_000;
    private static final int COURIER_LIMIT_GRAMS = 30_000;

    private final Map<String, Product> productsBySku = new HashMap<>();
    private final List<Shipment> shipments = new ArrayList<>();

    public void stock(Product product) {
        productsBySku.put(product.sku(), product);
    }

    public void on(OrderPlaced event) {
        var weight = 0;
        var fragile = false;
        for (var line : event.lines()) {
            var product = productsBySku.get(line.sku());
            weight += product.weightGrams() * line.quantity();
            fragile |= product.fragile();
        }
        shipments.add(new Shipment(event.orderId(), event.deliveryAddress(), weight, fragile, carrierFor(weight, fragile)));
    }

    private static Carrier carrierFor(int weightGrams, boolean fragile) {
        if (weightGrams > COURIER_LIMIT_GRAMS) {
            return Carrier.FREIGHT;
        }
        return weightGrams > POSTAL_LIMIT_GRAMS || fragile ? Carrier.COURIER : Carrier.POSTAL;
    }

    public List<Shipment> shipments() {
        return List.copyOf(shipments);
    }
}
