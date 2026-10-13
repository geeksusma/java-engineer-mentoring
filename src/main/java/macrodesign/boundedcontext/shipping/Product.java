package macrodesign.boundedcontext.shipping;

/**
 * Same word, different model: for Shipping a "Product" is a box with a weight
 * and handling needs. It has no price, because pricing means nothing here.
 */
public record Product(String sku, int weightGrams, boolean fragile) {
}
