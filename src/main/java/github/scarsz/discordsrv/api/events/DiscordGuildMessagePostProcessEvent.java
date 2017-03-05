package github.scarsz.discordsrv.api.events;

import github.scarsz.discordsrv.api.Cancellable;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

/**
 * <p>Called directly after a Discord message was processed but before being broadcasted to the server</p>
 *
 * <p>At the time this event is called, {@link #getProcessedMessage()} would return what the final message
 * would look like in-game, including text like the author before the actual message to which you could use
 * {@link #setProcessedMessage(String)} to change the message that would be broadcasted in-game or
 * {@link #setCancelled(boolean)} to cancel it from being broadcasted altogether</p>
 */
public class DiscordGuildMessagePostProcessEvent extends DiscordEvent<GuildMessageReceivedEvent> implements Cancellable {

    @Getter @Setter private boolean cancelled;

    @Getter private final User author;
    @Getter private final TextChannel channel;
    @Getter private final Guild guild;
    @Getter private final Member member;
    @Getter private final Message message;

    @Getter @Setter private String processedMessage;

    public DiscordGuildMessagePostProcessEvent(GuildMessageReceivedEvent jdaEvent, boolean cancelled, String processedMessage) {
        super(jdaEvent.getJDA(), jdaEvent);
        this.author = jdaEvent.getAuthor();
        this.channel = jdaEvent.getChannel();
        this.guild = jdaEvent.getGuild();
        this.member = jdaEvent.getMember();
        this.message = jdaEvent.getMessage();

        this.setCancelled(cancelled);
        this.processedMessage = processedMessage;
    }

}
