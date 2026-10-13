package macrodesign.modular.catalog.server;

import macrodesign.modular.catalog.api.CatalogApi;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * What you add when the catalog module is extracted into its own service: an
 * HTTP-ish endpoint in front of the very same {@link CatalogApi}. The body
 * format is one {@code sku;name;price} line per product found.
 */
public class CatalogEndpoint {

    private final CatalogApi catalog;

    public CatalogEndpoint(CatalogApi catalog) {
        this.catalog = catalog;
    }

    public String get(String path) {
        var skus = Arrays.asList(path.substring("/products?skus=".length()).split(","));
        return catalog.findProducts(skus).values().stream()
                .map(product -> product.sku() + ";" + product.name() + ";" + product.price())
                .sorted()
                .collect(Collectors.joining("\n"));
    }
}
