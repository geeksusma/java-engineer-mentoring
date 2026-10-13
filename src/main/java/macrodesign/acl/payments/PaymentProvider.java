package macrodesign.acl.payments;

/**
 * The port our domain wants. It's shaped by our needs, not by the provider's API.
 */
public interface PaymentProvider {

    PaymentResult charge(Payment payment);
}
