package macrodesign.hexagonal.adapter.in.web;

import macrodesign.hexagonal.application.port.in.PlaceOrderCommand;
import macrodesign.hexagonal.application.port.in.PlaceOrderUseCase;
import macrodesign.hexagonal.domain.OrderRejectedException;
import macrodesign.hexagonal.domain.ProductCode;

/**
 * Driving adapter: translates "HTTP" into a call on the driving port. It knows
 * the use case interface, never the service class behind it.
 */
public class OrderController {

    private final PlaceOrderUseCase placeOrder;

    public OrderController(PlaceOrderUseCase placeOrder) {
        this.placeOrder = placeOrder;
    }

    public Response post(String customerEmail, String productCode, int quantity) {
        try {
            var order = placeOrder.placeOrder(new PlaceOrderCommand(customerEmail, new ProductCode(productCode), quantity));
            return new Response(201, "order #" + order.id().value() + " total " + order.total());
        } catch (OrderRejectedException e) {
            return new Response(400, e.getMessage());
        }
    }
}
