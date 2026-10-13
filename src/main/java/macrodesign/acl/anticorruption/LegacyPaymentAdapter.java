package macrodesign.acl.anticorruption;

import macrodesign.acl.legacy.LegacyPaymentTerminal;
import macrodesign.acl.legacy.TerminalException;
import macrodesign.acl.payments.Payment;
import macrodesign.acl.payments.PaymentProvider;
import macrodesign.acl.payments.PaymentResult;

/**
 * The anti-corruption layer: implements our port, talks to the legacy system
 * and translates everything that crosses the line — data, codes and errors.
 * This is the only class allowed to know the terminal exists.
 */
public class LegacyPaymentAdapter implements PaymentProvider {

    private final LegacyPaymentTerminal terminal;

    public LegacyPaymentAdapter(LegacyPaymentTerminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public PaymentResult charge(Payment payment) {
        try {
            var response = terminal.doTx(LegacyPaymentTranslator.toLegacyRequest(payment));
            return LegacyPaymentTranslator.toDomainResult(response);
        } catch (TerminalException e) {
            return new PaymentResult.ProviderUnavailable(e.getMessage());
        }
    }
}
