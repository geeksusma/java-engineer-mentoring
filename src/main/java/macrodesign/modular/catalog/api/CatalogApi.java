package macrodesign.modular.catalog.api;

import java.util.Collection;
import java.util.Map;

/**
 * The catalog module's public face. Everything else in {@code catalog} is an
 * implementation detail other modules must not touch.
 * <p>
 * Designed as if it could be remote one day: coarse-grained (a batch lookup)
 * and with an explicit failure mode.
 *
 * @throws CatalogUnavailableException when the catalog can't answer
 */
public interface CatalogApi {

    Map<String, ProductView> findProducts(Collection<String> skus);
}
