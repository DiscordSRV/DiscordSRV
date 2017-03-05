package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

/**
 * <p>Called directly after receiving a message through Discord from a {@link Guild} that was not sent by the bot</p>
 * <p>As this includes <i>every</i> message the bot receives, this event does
 * not necessarily guarantee the received message is from a linked chat channel</p>
 */
public class DiscordGuildMessageReceivedEvent extends DiscordEvent<GuildMessageReceivedEvent> {

    @Getter private final User author;
    @Getter private final TextChannel channel;
    @Getter private final Guild guild;
    @Getter private final Member member;
    @Getter private final Message message;

    public DiscordGuildMessageReceivedEvent(GuildMessageReceivedEvent jdaEvent) {
        super(jdaEvent.getJDA(), jdaEvent);
        this.author = jdaEvent.getAuthor();
        this.channel = jdaEvent.getChannel();
        this.guild = jdaEvent.getGuild();
        this.member = jdaEvent.getMember();
        this.message = jdaEvent.getMessage();
    }

}
