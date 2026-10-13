package macrodesign.clean.usecases;

import macrodesign.clean.entities.Nickname;
import macrodesign.clean.entities.Player;
import macrodesign.clean.entities.PlayerId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterPlayerInteractorTest {

    private final FakePlayers players = new FakePlayers();
    private final SpyPresenter presenter = new SpyPresenter();
    private final RegisterPlayerInteractor interactor = new RegisterPlayerInteractor(players, presenter);

    @Test
    void should_pushResponseToPresenter_when_registrationSucceeds() {
        interactor.register(new RegisterPlayerRequest("Neo", "neo@example.com"));

        assertEquals(List.of("registered P-1 Neo"), presenter.calls);
        assertEquals(1, players.saved.size());
    }

    @Test
    void should_presentNicknameTaken_when_nicknameDiffersOnlyInCase() {
        interactor.register(new RegisterPlayerRequest("Neo", "neo@example.com"));

        interactor.register(new RegisterPlayerRequest("NEO", "other@example.com"));

        assertEquals("taken NEO", presenter.calls.getLast());
        assertEquals(1, players.saved.size());
    }

    @Test
    void should_presentInvalidNickname_when_entityRuleIsBroken() {
        interactor.register(new RegisterPlayerRequest("x!", "x@example.com"));

        assertEquals(List.of("invalid nickname must be 3-16 letters, digits or underscores"), presenter.calls);
        assertTrue(players.saved.isEmpty());
    }

    private static final class FakePlayers implements PlayerGateway {

        final List<Player> saved = new ArrayList<>();

        @Override
        public boolean existsByNickname(Nickname nickname) {
            return saved.stream().anyMatch(player -> player.nickname().canonical().equals(nickname.canonical()));
        }

        @Override
        public PlayerId nextId() {
            return new PlayerId("P-" + (saved.size() + 1));
        }

        @Override
        public void save(Player player) {
            saved.add(player);
        }
    }

    private static final class SpyPresenter implements RegisterPlayerOutputBoundary {

        final List<String> calls = new ArrayList<>();

        @Override
        public void presentRegistered(RegisterPlayerResponse response) {
            calls.add("registered " + response.playerId() + " " + response.nickname());
        }

        @Override
        public void presentInvalidNickname(String reason) {
            calls.add("invalid " + reason);
        }

        @Override
        public void presentNicknameTaken(String nickname) {
            calls.add("taken " + nickname);
        }
    }
}
