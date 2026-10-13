package macrodesign.acl;

import macrodesign.acl.anticorruption.LegacyPaymentAdapter;
import macrodesign.acl.legacy.LegacyPaymentTerminal;
import macrodesign.acl.payments.Checkout;
import macrodesign.acl.payments.Checkout.Outcome;
import macrodesign.acl.payments.Money;
import macrodesign.acl.payments.Payment;
import macrodesign.acl.payments.PaymentResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckoutThroughAclTest {

    private final LegacyPaymentAdapter adapter = new LegacyPaymentAdapter(new LegacyPaymentTerminal());
    private final Checkout checkout = new Checkout(adapter);

    @ParameterizedTest
    @CsvSource({
            "tok_ok,     PAID",
            "tok_broke,  ASK_FOR_ANOTHER_CARD",
            "tok_stolen, ASK_FOR_ANOTHER_CARD",
            "tok_weird,  ASK_FOR_ANOTHER_CARD",
            "tok_down,   TRY_AGAIN_LATER"
    })
    void should_decideInDomainTerms_when_legacyTerminalAnswers(String cardToken, Outcome expected) {
        assertEquals(expected, checkout.pay("SO-1", Money.of("19.99", "EUR"), cardToken));
    }

    @Test
    void should_turnCheckedExceptionsIntoAResult_when_terminalFails() {
        var result = adapter.charge(new Payment("SO-1", Money.of("1", "EUR"), "tok_down"));

        assertEquals(new PaymentResult.ProviderUnavailable("ERR_COMMS_TIMEOUT"), result);
    }

    @Test
    void should_keepTheAuthorizationId_when_approved() {
        var result = adapter.charge(new Payment("SO-9", Money.of("5.00", "EUR"), "tok_ok"));

        assertEquals(new PaymentResult.Approved("ASO-9-500"), result);
    }
}
