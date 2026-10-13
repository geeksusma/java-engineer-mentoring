package macrodesign.clean.usecases;

/**
 * Request model: plain data crossing the boundary inwards. No entities, no
 * framework types — the controller can build it from anything.
 */
public record RegisterPlayerRequest(String nickname, String email) {
}
