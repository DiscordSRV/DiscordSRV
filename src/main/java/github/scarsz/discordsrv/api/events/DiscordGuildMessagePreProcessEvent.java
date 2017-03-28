package github.scarsz.discordsrv.api.events;

import github.scarsz.discordsrv.api.Cancellable;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

/**
 * <p>Called after {@link DiscordGuildMessageReceivedEvent} when the message was validated as coming from a linked channel</p>
 * <p>Guaranteed to be from a linked {@link TextChannel}</p>
 */
public class DiscordGuildMessagePreProcessEvent extends DiscordEvent<GuildMessageReceivedEvent> implements Cancellable {

    @Getter @Setter private boolean cancelled;

    @Getter private final User author;
    @Getter private final TextChannel channel;
    @Getter private final Guild guild;
    @Getter private final Member member;
    @Getter private final Message message;

    public DiscordGuildMessagePreProcessEvent(GuildMessageReceivedEvent jdaEvent) {
        super(jdaEvent.getJDA(), jdaEvent);
        this.author = jdaEvent.getAuthor();
        this.channel = jdaEvent.getChannel();
        this.guild = jdaEvent.getGuild();
        this.member = jdaEvent.getMember();
        this.message = jdaEvent.getMessage();
    }

}
