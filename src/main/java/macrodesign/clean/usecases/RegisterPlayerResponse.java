package macrodesign.clean.usecases;

/**
 * Response model: plain data crossing the boundary outwards. The entity itself
 * never leaves the use case.
 */
public record RegisterPlayerResponse(String playerId, String nickname) {
}
