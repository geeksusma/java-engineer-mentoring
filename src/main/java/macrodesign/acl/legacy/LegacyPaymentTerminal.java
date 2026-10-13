package macrodesign.acl.legacy;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulates a third-party SDK we can't change: stringly-typed maps, amounts
 * as strings of minor units, numeric ISO-4217 currency codes, two-character
 * response codes and a checked exception for anything technical.
 * <p>
 * The card token decides the outcome so tests are deterministic:
 * {@code tok_ok} approves, {@code tok_broke} lacks funds, {@code tok_stolen}
 * is refused, {@code tok_weird} returns a code we've never seen, and
 * {@code tok_down} makes the terminal fail.
 */
public class LegacyPaymentTerminal {

    public Map<String, String> doTx(Map<String, String> request) throws TerminalException {
        if (!"S".equals(request.get("TX_MODE")) || request.get("AMT_MINOR") == null || request.get("CCY_NUM") == null) {
            return Map.of("RC", "30"); // "format error"
        }
        var token = request.get("PAN_TOKEN");
        var response = new HashMap<String, String>();
        switch (token) {
            case "tok_ok" -> {
                response.put("RC", "00");
                response.put("AUTH_ID", "A" + request.get("MERCH_REF") + "-" + request.get("AMT_MINOR"));
            }
            case "tok_broke" -> response.put("RC", "51");
            case "tok_stolen" -> response.put("RC", "05");
            case "tok_weird" -> response.put("RC", "Z9");
            case "tok_down" -> throw new TerminalException("ERR_COMMS_TIMEOUT");
            default -> response.put("RC", "14"); // "invalid card"
        }
        return response;
    }
}
