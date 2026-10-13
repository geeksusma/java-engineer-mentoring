package macrodesign.nlayered.presentation;

/**
 * A minimal stand-in for an HTTP response, so the example needs no web framework.
 */
public record Response(int status, String body) {
}
