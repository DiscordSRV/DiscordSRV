/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.api.events;

import github.scarsz.discordsrv.api.Cancellable;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;


/**
 * <p>Called directly before a command is sent to the minecraft server from discord</p>
 *
 * <p>At the time this event is called, the command can still be changed, and event cancelled</p>
 * <p>Cancelling the event will stop the command from being sent to the server</p>
 */

public class DiscordConsoleCommandPreProcessEvent extends DiscordEvent<GuildMessageReceivedEvent> implements Cancellable{

    @Getter private boolean sentInConsoleChannel;
    @Getter @Setter private boolean cancelled;
    @Getter @Setter private String command;

    public DiscordConsoleCommandPreProcessEvent(GuildMessageReceivedEvent jdaEvent, String command, boolean sentInConsoleChannel) {
        super(jdaEvent.getJDA(), jdaEvent);
        this.sentInConsoleChannel = sentInConsoleChannel;
        this.cancelled = false;
        this.command = command;
    }
}
