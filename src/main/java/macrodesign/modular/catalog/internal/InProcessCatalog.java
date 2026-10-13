package macrodesign.modular.catalog.internal;

import macrodesign.modular.catalog.api.CatalogApi;
import macrodesign.modular.catalog.api.ProductView;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InProcessCatalog implements CatalogApi {

    private final Map<String, ProductEntity> products = new HashMap<>();

    public void add(ProductEntity product) {
        products.put(product.sku(), product);
    }

    @Override
    public Map<String, ProductView> findProducts(Collection<String> skus) {
        var found = new HashMap<String, ProductView>();
        for (var sku : skus) {
            var entity = products.get(sku);
            if (entity != null) {
                found.put(sku, new ProductView(entity.sku(), entity.name(), entity.price()));
            }
        }
        return found;
    }
}
