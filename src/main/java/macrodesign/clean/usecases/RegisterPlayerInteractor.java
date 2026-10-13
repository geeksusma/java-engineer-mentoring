package macrodesign.clean.usecases;

import macrodesign.clean.entities.InvalidNicknameException;
import macrodesign.clean.entities.Nickname;
import macrodesign.clean.entities.Player;

/**
 * Application business rule: the steps of "register a player". It orchestrates
 * entities and gateways and tells the presenter what happened.
 */
public class RegisterPlayerInteractor implements RegisterPlayerInputBoundary {

    private final PlayerGateway players;
    private final RegisterPlayerOutputBoundary presenter;

    public RegisterPlayerInteractor(PlayerGateway players, RegisterPlayerOutputBoundary presenter) {
        this.players = players;
        this.presenter = presenter;
    }

    @Override
    public void register(RegisterPlayerRequest request) {
        Nickname nickname;
        try {
            nickname = new Nickname(request.nickname());
        } catch (InvalidNicknameException e) {
            presenter.presentInvalidNickname(e.getMessage());
            return;
        }
        if (players.existsByNickname(nickname)) {
            presenter.presentNicknameTaken(nickname.value());
            return;
        }
        var player = new Player(players.nextId(), nickname, request.email());
        players.save(player);
        presenter.presentRegistered(new RegisterPlayerResponse(player.id().value(), nickname.value()));
    }
}
