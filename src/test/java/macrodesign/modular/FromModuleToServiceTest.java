package macrodesign.modular;

import macrodesign.modular.catalog.CatalogModule;
import macrodesign.modular.catalog.client.RemoteCatalogClient;
import macrodesign.modular.catalog.server.CatalogEndpoint;
import macrodesign.modular.ordering.OrderResult;
import macrodesign.modular.ordering.OrderingModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FromModuleToServiceTest {

    private static final Map<String, Integer> CART = Map.of("KB-01", 2, "MS-02", 1);

    @Test
    void should_placeOrder_when_catalogIsAModuleInTheSameProcess() {
        var ordering = new OrderingModule(CatalogModule.withSampleProducts());

        assertEquals(new OrderResult.Placed(new BigDecimal("129.70")), ordering.placeOrder(CART));
        assertEquals(new OrderResult.Rejected("unknown product: NOPE"), ordering.placeOrder(Map.of("NOPE", 1)));
    }

    @Test
    void should_giveTheSameAnswer_when_catalogIsExtractedBehindTheNetwork() {
        var catalogService = new CatalogEndpoint(CatalogModule.withSampleProducts());
        var ordering = new OrderingModule(new RemoteCatalogClient(catalogService::get));

        assertEquals(new OrderResult.Placed(new BigDecimal("129.70")), ordering.placeOrder(CART));
    }

    @Test
    void should_askToRetryLater_when_theNetworkFails() {
        var ordering = new OrderingModule(new RemoteCatalogClient(path -> {
            throw new SocketTimeoutException("read timed out");
        }));

        var result = ordering.placeOrder(CART);

        assertInstanceOf(OrderResult.RetryLater.class, result);
    }

    @Test
    void should_payOneRoundTripPerCall_when_catalogIsRemote() {
        var catalogService = new CatalogEndpoint(CatalogModule.withSampleProducts());
        var requests = new ArrayList<String>();
        var client = new RemoteCatalogClient(path -> {
            requests.add(path);
            return catalogService.get(path);
        });

        new OrderingModule(client).placeOrder(CART);
        assertEquals(1, requests.size());

        // a chatty, one-product-at-a-time style multiplies the round trips: harmless
        // as an in-process call, a latency and failure multiplier over the network
        requests.clear();
        for (var sku : CART.keySet()) {
            client.findProducts(List.of(sku));
        }
        assertEquals(CART.size(), requests.size());
    }
}
