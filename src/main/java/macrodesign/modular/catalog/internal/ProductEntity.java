package macrodesign.modular.catalog.internal;

import java.math.BigDecimal;

/**
 * Internal model of the catalog module: it carries data (supplier, cost) that
 * nobody outside the module should build logic on.
 */
public record ProductEntity(String sku, String name, BigDecimal price, String supplier, BigDecimal cost) {
}
