package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;

/**
 * <p>Called directly after a message is sent to a {@link PrivateChannel} by the bot</p>
 */
public class DiscordPrivateMessageSentEvent extends DiscordEvent {

    @Getter private final PrivateChannel channel;
    @Getter private final Message message;
    @Getter private final User recipient;

    public DiscordPrivateMessageSentEvent(JDA jda, Message message) {
        super(jda);
        this.channel = message.getPrivateChannel();
        this.message = message;
        this.recipient = channel.getUser();
    }

}
