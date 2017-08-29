package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;

abstract class GameEvent extends Event {

    @Getter final private Player player;

    GameEvent(Player player) {
        this.player = player;
    }

}
