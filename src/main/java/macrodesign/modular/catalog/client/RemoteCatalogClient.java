package macrodesign.modular.catalog.client;

import macrodesign.modular.catalog.api.CatalogApi;
import macrodesign.modular.catalog.api.CatalogUnavailableException;
import macrodesign.modular.catalog.api.ProductView;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the same {@link CatalogApi} over the network, so the ordering
 * module doesn't change when the catalog becomes a microservice. What does
 * change: every call can now fail, and every call costs a round trip.
 */
public class RemoteCatalogClient implements CatalogApi {

    private final Transport transport;

    public RemoteCatalogClient(Transport transport) {
        this.transport = transport;
    }

    @Override
    public Map<String, ProductView> findProducts(Collection<String> skus) {
        String body;
        try {
            body = transport.get("/products?skus=" + String.join(",", skus));
        } catch (IOException e) {
            throw new CatalogUnavailableException("catalog service unreachable", e);
        }
        var found = new HashMap<String, ProductView>();
        body.lines().filter(line -> !line.isBlank()).forEach(line -> {
            var fields = line.split(";");
            found.put(fields[0], new ProductView(fields[0], fields[1], new BigDecimal(fields[2])));
        });
        return found;
    }
}
