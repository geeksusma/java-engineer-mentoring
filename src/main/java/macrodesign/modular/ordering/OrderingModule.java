package macrodesign.modular.ordering;

import macrodesign.modular.catalog.api.CatalogApi;
import macrodesign.modular.catalog.api.CatalogUnavailableException;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The ordering module only knows {@link CatalogApi}. Whether the catalog is a
 * package next door or a service across the network is a wiring decision.
 */
public class OrderingModule {

    private final CatalogApi catalog;

    public OrderingModule(CatalogApi catalog) {
        this.catalog = catalog;
    }

    public OrderResult placeOrder(Map<String, Integer> quantitiesBySku) {
        try {
            // one coarse-grained call, not one call per line
            var products = catalog.findProducts(quantitiesBySku.keySet());
            var total = BigDecimal.ZERO;
            for (var line : quantitiesBySku.entrySet()) {
                var product = products.get(line.getKey());
                if (product == null) {
                    return new OrderResult.Rejected("unknown product: " + line.getKey());
                }
                total = total.add(product.price().multiply(BigDecimal.valueOf(line.getValue())));
            }
            return new OrderResult.Placed(total);
        } catch (CatalogUnavailableException e) {
            return new OrderResult.RetryLater(e.getMessage());
        }
    }
}
