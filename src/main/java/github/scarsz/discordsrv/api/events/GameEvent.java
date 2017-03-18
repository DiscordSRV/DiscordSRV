package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/4/2017
 * @at 11:37 PM
 */

abstract class GameEvent extends Event {

    @Getter final private Player player;

    GameEvent(Player player) {
        this.player = player;
    }

}
