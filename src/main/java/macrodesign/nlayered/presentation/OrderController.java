package macrodesign.nlayered.presentation;

import macrodesign.nlayered.business.OrderRejectedException;
import macrodesign.nlayered.business.OrderService;

public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public Response post(String customerEmail, String productCode, int quantity) {
        try {
            var order = orderService.placeOrder(customerEmail, productCode, quantity);
            return new Response(201, "order #" + order.id() + " total " + order.total());
        } catch (OrderRejectedException e) {
            return new Response(400, e.getMessage());
        }
    }
}
