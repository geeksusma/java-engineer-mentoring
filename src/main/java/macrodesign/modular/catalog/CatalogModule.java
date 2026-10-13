package macrodesign.modular.catalog;

import macrodesign.modular.catalog.internal.InProcessCatalog;
import macrodesign.modular.catalog.internal.ProductEntity;

import java.math.BigDecimal;

/**
 * Module entry point used by the composition root. Without JPMS, Java can't
 * hide {@code internal} from other packages, so the boundary is enforced by an
 * architecture test instead (see ModuleBoundariesTest).
 */
public final class CatalogModule {

    private CatalogModule() {
    }

    public static InProcessCatalog withSampleProducts() {
        var catalog = new InProcessCatalog();
        catalog.add(new ProductEntity("KB-01", "Mechanical keyboard", new BigDecimal("49.90"), "KeyCo", new BigDecimal("21.00")));
        catalog.add(new ProductEntity("MS-02", "Gaming mouse", new BigDecimal("29.90"), "ClickCo", new BigDecimal("9.50")));
        return catalog;
    }
}
