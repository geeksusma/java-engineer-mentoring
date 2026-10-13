package macrodesign.hexagonal.domain;

import java.math.BigDecimal;

public record Order(OrderId id, String customerEmail, ProductCode productCode, int quantity, BigDecimal total) {
}
