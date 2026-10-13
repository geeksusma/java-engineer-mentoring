package macrodesign.acl.payments;

/**
 * Domain code reads like the business talks, with no trace of the legacy
 * terminal. The exhaustive switch over the sealed result means a new outcome
 * can't be forgotten.
 */
public class Checkout {

    public enum Outcome {
        PAID,
        ASK_FOR_ANOTHER_CARD,
        TRY_AGAIN_LATER
    }

    private final PaymentProvider payments;

    public Checkout(PaymentProvider payments) {
        this.payments = payments;
    }

    public Outcome pay(String orderId, Money amount, String cardToken) {
        return switch (payments.charge(new Payment(orderId, amount, cardToken))) {
            case PaymentResult.Approved approved -> Outcome.PAID;
            case PaymentResult.Declined declined -> Outcome.ASK_FOR_ANOTHER_CARD;
            case PaymentResult.ProviderUnavailable unavailable -> Outcome.TRY_AGAIN_LATER;
        };
    }
}
