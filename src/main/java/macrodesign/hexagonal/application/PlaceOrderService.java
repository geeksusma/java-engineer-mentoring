package macrodesign.hexagonal.application;

import macrodesign.hexagonal.application.port.in.PlaceOrderCommand;
import macrodesign.hexagonal.application.port.in.PlaceOrderUseCase;
import macrodesign.hexagonal.application.port.out.OrderConfirmationSender;
import macrodesign.hexagonal.application.port.out.OrderRepository;
import macrodesign.hexagonal.application.port.out.ProductCatalog;
import macrodesign.hexagonal.domain.Order;
import macrodesign.hexagonal.domain.OrderRejectedException;

/**
 * Same use case as the N-layered OrderService, but every dependency is a port
 * owned by the application. The arrows now point inwards.
 */
public class PlaceOrderService implements PlaceOrderUseCase {

    private final ProductCatalog catalog;
    private final OrderRepository orders;
    private final OrderConfirmationSender confirmations;

    public PlaceOrderService(ProductCatalog catalog, OrderRepository orders, OrderConfirmationSender confirmations) {
        this.catalog = catalog;
        this.orders = orders;
        this.confirmations = confirmations;
    }

    @Override
    public Order placeOrder(PlaceOrderCommand command) {
        var product = catalog.findByCode(command.productCode())
                .orElseThrow(() -> new OrderRejectedException("unknown product: " + command.productCode().value()));

        catalog.save(product.reserve(command.quantity()));
        var order = new Order(orders.nextId(), command.customerEmail(), product.code(), command.quantity(),
                product.priceFor(command.quantity()));
        orders.save(order);
        confirmations.sendConfirmation(order);
        return order;
    }
}
