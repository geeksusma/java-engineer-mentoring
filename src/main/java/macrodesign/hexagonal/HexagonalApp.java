package macrodesign.hexagonal;

import macrodesign.hexagonal.adapter.in.cli.OrderCommandLine;
import macrodesign.hexagonal.adapter.in.web.OrderController;
import macrodesign.hexagonal.adapter.out.notification.OutboxConfirmationSender;
import macrodesign.hexagonal.adapter.out.persistence.InMemoryOrderRepository;
import macrodesign.hexagonal.adapter.out.persistence.InMemoryProductCatalog;
import macrodesign.hexagonal.application.PlaceOrderService;

/**
 * Composition root: the only place that knows every concrete class and plugs
 * adapters into ports. In Spring this is what the container does for you.
 */
public final class HexagonalApp {

    public final InMemoryProductCatalog catalog = new InMemoryProductCatalog();
    public final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    public final OutboxConfirmationSender confirmations = new OutboxConfirmationSender();

    private final PlaceOrderService placeOrder = new PlaceOrderService(catalog, orders, confirmations);

    public final OrderController web = new OrderController(placeOrder);
    public final OrderCommandLine cli = new OrderCommandLine(placeOrder);
}
