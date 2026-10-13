package macrodesign.modular.catalog.api;

import java.math.BigDecimal;

/**
 * What the catalog module publishes about a product. Other modules get this,
 * never the catalog's internal entity.
 */
public record ProductView(String sku, String name, BigDecimal price) {
}
