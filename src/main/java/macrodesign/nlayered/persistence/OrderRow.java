package macrodesign.nlayered.persistence;

import java.math.BigDecimal;

public record OrderRow(long id, String customerEmail, String productCode, int quantity, BigDecimal total) {
}
