package macrodesign.hexagonal.application.port.in;

import macrodesign.hexagonal.domain.ProductCode;

public record PlaceOrderCommand(String customerEmail, ProductCode productCode, int quantity) {
}
