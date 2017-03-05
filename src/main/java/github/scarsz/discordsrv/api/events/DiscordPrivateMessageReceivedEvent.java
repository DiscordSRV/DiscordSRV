package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

/**
 * <p>Called directly after receiving a message through Discord from a {@link PrivateChannel}</p>
 */
public class DiscordPrivateMessageReceivedEvent extends DiscordEvent<PrivateMessageReceivedEvent> {

    @Getter private final User author;
    @Getter private final PrivateChannel channel;
    @Getter private final Message message;

    public DiscordPrivateMessageReceivedEvent(PrivateMessageReceivedEvent jdaEvent) {
        super(jdaEvent.getJDA(), jdaEvent);
        this.author = jdaEvent.getAuthor();
        this.channel = jdaEvent.getChannel();
        this.message = jdaEvent.getMessage();
    }

}
