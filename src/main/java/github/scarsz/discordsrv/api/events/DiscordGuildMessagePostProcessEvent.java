/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.api.events;

import github.scarsz.discordsrv.api.Cancellable;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
