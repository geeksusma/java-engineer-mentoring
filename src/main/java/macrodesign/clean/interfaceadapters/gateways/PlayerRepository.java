package macrodesign.clean.interfaceadapters.gateways;

import macrodesign.clean.entities.Nickname;
import macrodesign.clean.entities.Player;
import macrodesign.clean.entities.PlayerId;
import macrodesign.clean.frameworks.database.PlayerTable;
import macrodesign.clean.usecases.PlayerGateway;

/**
 * Interface adapter: implements the gateway the use case owns and translates
 * between entities and the database's rows.
 */
public class PlayerRepository implements PlayerGateway {

    private final PlayerTable table;

    public PlayerRepository(PlayerTable table) {
        this.table = table;
    }

    @Override
    public boolean existsByNickname(Nickname nickname) {
        return table.findByNicknameKey(nickname.canonical()).isPresent();
    }

    @Override
    public PlayerId nextId() {
        return new PlayerId(table.nextId());
    }

    @Override
    public void save(Player player) {
        table.insert(new PlayerTable.Row(player.id().value(), player.nickname().value(),
                player.nickname().canonical(), player.email()));
    }
}
