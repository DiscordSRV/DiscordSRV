package github.scarsz.discordsrv.api.events;

import lombok.Getter;

/**
 * <p>Called directly after a Discord message was processed and was broadcasted to the server</p>
 *
 * <p>At the time this event is called, {@link #getProcessedMessage()} would return what the final message
 * would look like in-game, including text like the author before the actual message</p>
 */
public class DiscordGuildMessagePostBroadcastEvent extends Event {

    @Getter private final String channel;
    @Getter private final String processedMessage;

    public DiscordGuildMessagePostBroadcastEvent(String channel, String processedMessage) {
        this.channel = channel;
        this.processedMessage = processedMessage;
    }

}
