package macrodesign.clean.usecases;

/**
 * The use case doesn't return a result, it pushes it to the presenter through
 * this interface. That's how control flows outwards while the source-code
 * dependency still points inwards: the presenter implements an interface the
 * use case owns.
 */
public interface RegisterPlayerOutputBoundary {

    void presentRegistered(RegisterPlayerResponse response);

    void presentInvalidNickname(String reason);

    void presentNicknameTaken(String nickname);
}
