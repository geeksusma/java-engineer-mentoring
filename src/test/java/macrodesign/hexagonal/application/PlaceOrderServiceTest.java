package macrodesign.hexagonal.application;

import macrodesign.hexagonal.application.port.in.PlaceOrderCommand;
import macrodesign.hexagonal.application.port.out.OrderConfirmationSender;
import macrodesign.hexagonal.application.port.out.OrderRepository;
import macrodesign.hexagonal.application.port.out.ProductCatalog;
import macrodesign.hexagonal.domain.Order;
import macrodesign.hexagonal.domain.OrderId;
import macrodesign.hexagonal.domain.OrderRejectedException;
import macrodesign.hexagonal.domain.Product;
import macrodesign.hexagonal.domain.ProductCode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The core is tested with tiny fakes written right here: no database, no web,
 * no adapters at all. That is the payoff of owning the ports.
 */
class PlaceOrderServiceTest {

    private static final ProductCode KEYBOARD = new ProductCode("KB-01");

    private final FakeCatalog catalog = new FakeCatalog();
    private final List<Order> savedOrders = new ArrayList<>();
    private final List<Order> confirmed = new ArrayList<>();

    private final OrderRepository orders = new OrderRepository() {
        @Override
        public OrderId nextId() {
            return new OrderId(42);
        }

        @Override
        public void save(Order order) {
            savedOrders.add(order);
        }
    };
    private final OrderConfirmationSender confirmations = confirmed::add;

    private final PlaceOrderService service = new PlaceOrderService(catalog, orders, confirmations);

    @Test
    void should_reserveStockSaveAndConfirm_when_orderIsValid() {
        catalog.save(new Product(KEYBOARD, new BigDecimal("49.90"), 3));

        var order = service.placeOrder(new PlaceOrderCommand("ana@example.com", KEYBOARD, 2));

        assertEquals(new Order(new OrderId(42), "ana@example.com", KEYBOARD, 2, new BigDecimal("99.80")), order);
        assertEquals(1, catalog.findByCode(KEYBOARD).orElseThrow().stock());
        assertEquals(List.of(order), savedOrders);
        assertEquals(List.of(order), confirmed);
    }

    @Test
    void should_touchNoOutputPort_when_stockIsInsufficient() {
        catalog.save(new Product(KEYBOARD, new BigDecimal("49.90"), 1));

        assertThrows(OrderRejectedException.class,
                () -> service.placeOrder(new PlaceOrderCommand("ana@example.com", KEYBOARD, 2)));

        assertEquals(1, catalog.findByCode(KEYBOARD).orElseThrow().stock());
        assertTrue(savedOrders.isEmpty());
        assertTrue(confirmed.isEmpty());
    }

    private static final class FakeCatalog implements ProductCatalog {

        private final Map<ProductCode, Product> products = new HashMap<>();

        @Override
        public Optional<Product> findByCode(ProductCode code) {
            return Optional.ofNullable(products.get(code));
        }

        @Override
        public void save(Product product) {
            products.put(product.code(), product);
        }
    }
}
