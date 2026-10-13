package macrodesign.acl.anticorruption;

import macrodesign.acl.payments.DeclineReason;
import macrodesign.acl.payments.Money;
import macrodesign.acl.payments.Payment;
import macrodesign.acl.payments.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyPaymentTranslatorTest {

    @Test
    void should_speakTheLegacyDialect_when_translatingAPayment() {
        var request = LegacyPaymentTranslator.toLegacyRequest(new Payment("SO-7", Money.of("12.34", "EUR"), "tok_ok"));

        assertEquals(Map.of(
                "TX_MODE", "S",
                "MERCH_REF", "SO-7",
                "AMT_MINOR", "1234",
                "CCY_NUM", "978",
                "PAN_TOKEN", "tok_ok"), request);
    }

    @ParameterizedTest
    @CsvSource({
            "10,     EUR, 1000",
            "0.995,  EUR, 100",
            "1500,   JPY, 1500",
            "1.234,  BHD, 1234"
    })
    void should_respectEachCurrencysMinorUnits_when_convertingAmounts(String amount, String currency, String minor) {
        var request = LegacyPaymentTranslator.toLegacyRequest(new Payment("SO-1", Money.of(amount, currency), "tok_ok"));

        assertEquals(minor, request.get("AMT_MINOR"));
    }

    @Test
    void should_mapResponseCodesToOurLanguage_when_translatingBack() {
        assertEquals(new PaymentResult.Approved("A-1"), LegacyPaymentTranslator.toDomainResult(Map.of("RC", "00", "AUTH_ID", "A-1")));
        assertEquals(new PaymentResult.Declined(DeclineReason.INSUFFICIENT_FUNDS), LegacyPaymentTranslator.toDomainResult(Map.of("RC", "51")));
        assertEquals(new PaymentResult.Declined(DeclineReason.REFUSED_BY_ISSUER), LegacyPaymentTranslator.toDomainResult(Map.of("RC", "05")));
        assertEquals(new PaymentResult.ProviderUnavailable("issuer or switch down"), LegacyPaymentTranslator.toDomainResult(Map.of("RC", "91")));
    }

    @Test
    void should_failSafe_when_codeIsUnknown() {
        assertEquals(new PaymentResult.Declined(DeclineReason.UNKNOWN), LegacyPaymentTranslator.toDomainResult(Map.of("RC", "Z9")));
        assertEquals(new PaymentResult.Declined(DeclineReason.UNKNOWN), LegacyPaymentTranslator.toDomainResult(Map.of()));
    }
}
