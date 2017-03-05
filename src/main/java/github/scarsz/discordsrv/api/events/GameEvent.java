package github.scarsz.discordsrv.api.events;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/4/2017
 * @at 11:37 PM
 */

import lombok.Getter;
import org.bukkit.entity.Player;

public abstract class GameEvent extends Event {

    @Getter final private Player player;

    GameEvent(Player player) {
        this.player = player;
    }

}
