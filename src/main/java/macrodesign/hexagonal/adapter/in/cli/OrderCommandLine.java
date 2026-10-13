package macrodesign.hexagonal.adapter.in.cli;

import macrodesign.hexagonal.application.port.in.PlaceOrderCommand;
import macrodesign.hexagonal.application.port.in.PlaceOrderUseCase;
import macrodesign.hexagonal.domain.OrderRejectedException;
import macrodesign.hexagonal.domain.ProductCode;

/**
 * A second driving adapter on the same port: {@code order <email> <code> <qty>}.
 * Adding a new way in (CLI, queue consumer, scheduled job) never touches the core.
 */
public class OrderCommandLine {

    private final PlaceOrderUseCase placeOrder;

    public OrderCommandLine(PlaceOrderUseCase placeOrder) {
        this.placeOrder = placeOrder;
    }

    public String run(String line) {
        var args = line.trim().split("\\s+");
        if (args.length != 4 || !args[0].equals("order")) {
            return "usage: order <email> <product-code> <quantity>";
        }
        try {
            var order = placeOrder.placeOrder(
                    new PlaceOrderCommand(args[1], new ProductCode(args[2]), Integer.parseInt(args[3])));
            return "OK " + order.id().value();
        } catch (OrderRejectedException e) {
            return "REJECTED " + e.getMessage();
        }
    }
}
