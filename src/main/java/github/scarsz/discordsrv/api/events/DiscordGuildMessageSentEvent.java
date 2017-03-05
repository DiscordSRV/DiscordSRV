package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

/**
 * <p>Called directly after a message is sent to a {@link TextChannel} by the bot</p>
 */
public class DiscordGuildMessageSentEvent extends DiscordEvent {

    @Getter private final TextChannel channel;
    @Getter private final Guild guild;
    @Getter private final Message message;

    public DiscordGuildMessageSentEvent(JDA jda, Message message) {
        super(jda);
        this.channel = message.getTextChannel();
        this.guild = message.getGuild();
        this.message = message;
    }

}
