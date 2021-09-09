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

import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kyori.adventure.text.Component;


/**
 * <p>Called directly after a command was sent to the minecraft server from discord</p>
 *
 * <p>{@link #getCommand()} returns the final command that was sent to the server
 * <p>{@link #isSentInConsoleChannel()} returns true if the message was sent in your console command discord channel, and false otherwise</p>
 */
public class DiscordConsoleCommandPostProcessEvent extends DiscordEvent<GuildMessageReceivedEvent>{

    /**
     * Whether or not it was sent in your guilds console channel
     */
    @Getter private boolean sentInConsoleChannel;

    /**
     * The command that was sent to the minecraft server from discord
     */
    @Getter private String command;

    public DiscordConsoleCommandPostProcessEvent(GuildMessageReceivedEvent jdaEvent, String command, boolean sentInConsoleChannel) {
        super(jdaEvent.getJDA(), jdaEvent);
        this.command = command;
        this.sentInConsoleChannel = sentInConsoleChannel;
    }
}
