package macrodesign.acl.payments;

/**
 * Our language for what can happen to a payment. Compare it with a map
 * holding {@code RC=51}.
 */
public sealed interface PaymentResult {

    record Approved(String authorizationId) implements PaymentResult {
    }

    record Declined(DeclineReason reason) implements PaymentResult {
    }

    record ProviderUnavailable(String detail) implements PaymentResult {
    }
}
