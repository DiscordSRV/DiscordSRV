package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/4/2017
 * @at 11:35 PM
 */
public class GameChatMessagePostProcessEvent extends GameEvent implements Cancellable {

    @Getter @Setter private boolean cancelled;

    @Getter @Setter private final String channel;
    @Getter @Setter private final String processedMessage;

    public GameChatMessagePostProcessEvent(String channel, String processedMessage, Player player, boolean cancelled) {
        super(player);
        this.channel = channel;
        this.processedMessage = processedMessage;
        setCancelled(cancelled);
    }

}
