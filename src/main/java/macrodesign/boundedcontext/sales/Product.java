package macrodesign.boundedcontext.sales;

import java.math.BigDecimal;

/**
 * "Product" as Sales understands it: something with a name and a price that
 * a customer can buy. Sales has no idea how heavy it is.
 */
public record Product(String sku, String name, BigDecimal price) {
}
