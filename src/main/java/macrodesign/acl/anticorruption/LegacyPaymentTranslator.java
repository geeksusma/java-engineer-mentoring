package macrodesign.acl.anticorruption;

import macrodesign.acl.payments.DeclineReason;
import macrodesign.acl.payments.Payment;
import macrodesign.acl.payments.PaymentResult;

import java.math.RoundingMode;
import java.util.Map;

/**
 * Pure translation, both directions, and nothing else — so every quirk of the
 * legacy model is documented (and tested) in one place.
 */
public final class LegacyPaymentTranslator {

    private LegacyPaymentTranslator() {
    }

    public static Map<String, String> toLegacyRequest(Payment payment) {
        var currency = payment.amount().currency();
        var minorUnits = payment.amount().amount()
                .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN)
                .movePointRight(currency.getDefaultFractionDigits());
        return Map.of(
                "TX_MODE", "S",                                                  // S = sale
                "MERCH_REF", payment.orderId(),
                "AMT_MINOR", minorUnits.toPlainString(),                         // 12.34 EUR -> "1234"
                "CCY_NUM", String.format("%03d", currency.getNumericCode()),      // EUR -> "978"
                "PAN_TOKEN", payment.cardToken());
    }

    public static PaymentResult toDomainResult(Map<String, String> response) {
        return switch (response.getOrDefault("RC", "")) {
            case "00" -> new PaymentResult.Approved(response.get("AUTH_ID"));
            case "51" -> new PaymentResult.Declined(DeclineReason.INSUFFICIENT_FUNDS);
            case "05" -> new PaymentResult.Declined(DeclineReason.REFUSED_BY_ISSUER);
            case "14" -> new PaymentResult.Declined(DeclineReason.INVALID_CARD);
            case "91", "96" -> new PaymentResult.ProviderUnavailable("issuer or switch down");
            default -> new PaymentResult.Declined(DeclineReason.UNKNOWN);
        };
    }
}
