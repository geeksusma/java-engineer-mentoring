package macrodesign.modular.catalog.client;

import java.io.IOException;

/**
 * The network. In production an HTTP client; in tests a lambda that can be
 * slow, fail, or count round trips.
 */
@FunctionalInterface
public interface Transport {

    String get(String path) throws IOException;
}
