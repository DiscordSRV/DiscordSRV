package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class GameChatMessagePostProcessEvent extends GameEvent implements Cancellable {

    @Getter @Setter private boolean cancelled;

    @Getter @Setter private String channel;
    @Getter @Setter private String processedMessage;

    public GameChatMessagePostProcessEvent(String channel, String processedMessage, Player player, boolean cancelled) {
        super(player);
        this.channel = channel;
        this.processedMessage = processedMessage;
        setCancelled(cancelled);
    }

}
