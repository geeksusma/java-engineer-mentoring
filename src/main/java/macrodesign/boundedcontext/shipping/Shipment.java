package macrodesign.boundedcontext.shipping;

/**
 * What Sales calls an "Order" is, for Shipping, a Shipment to be dispatched.
 */
public record Shipment(String orderReference, String recipientAddress, int totalWeightGrams, boolean handleWithCare,
                       Carrier carrier) {
}
