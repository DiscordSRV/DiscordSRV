package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * <p>Called directly after a Discord message was processed but before being broadcasted to the server</p>
 *
 * <p>At the time this event is called, {@link #getMessage()} would return what the person <i>said</i>, not
 * the final message. You could change what they said using the {@link #setMessage(String)} method or use
 * {@link #setCancelled(boolean)} to cancel it from being processed altogether</p>
 */
public class GameChatMessagePreProcessEvent extends GameEvent implements Cancellable {

    @Getter @Setter private boolean cancelled;

    @Getter @Setter private String channel;
    @Getter @Setter private String message;

    public GameChatMessagePreProcessEvent(String channel, String message, Player player) {
        super(player);
        this.channel = channel;
        this.message = message;
    }

}
