package macrodesign.nlayered.persistence;

import java.math.BigDecimal;

/**
 * One row of the PRODUCTS table. In a classic layered app this shape travels
 * all the way up to the business layer, so the table design becomes the
 * domain model.
 */
public record ProductRow(String code, BigDecimal unitPrice, int stock) {
}
