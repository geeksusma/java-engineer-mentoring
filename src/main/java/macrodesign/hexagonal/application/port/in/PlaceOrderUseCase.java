package macrodesign.hexagonal.application.port.in;

import macrodesign.hexagonal.domain.Order;

/**
 * Driving (primary) port: what the outside world can ask the application to do.
 */
public interface PlaceOrderUseCase {

    Order placeOrder(PlaceOrderCommand command);
}
