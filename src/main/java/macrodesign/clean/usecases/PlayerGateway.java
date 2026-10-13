package macrodesign.clean.usecases;

import macrodesign.clean.entities.Nickname;
import macrodesign.clean.entities.Player;
import macrodesign.clean.entities.PlayerId;

public interface PlayerGateway {

    boolean existsByNickname(Nickname nickname);

    PlayerId nextId();

    void save(Player player);
}
