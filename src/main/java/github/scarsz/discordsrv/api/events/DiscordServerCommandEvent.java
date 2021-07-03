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
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;


//This event gets called right before a command gets run
//If badCommand is true after the event has finished, the command is ignored and NOT sent to the minecraft server
public class DiscordServerCommandEvent extends DiscordEvent<GuildMessageReceivedEvent> {
    @Getter private TextChannel channel;
    @Getter private boolean commandSentInCommandsChannel;
    @Getter @Setter private static boolean badCommand;
    @Getter @Setter private Message message;
    @Getter private User author;
    @Getter private Member member;
    @Getter private Guild guild;
    @Getter private JDA jda;
    public DiscordServerCommandEvent(GuildMessageReceivedEvent jdaEvent, boolean commandSentInCommandsChannel) {
        super(jdaEvent.getJDA(), jdaEvent);
        DiscordServerCommandEvent.badCommand = false;
        this.commandSentInCommandsChannel = commandSentInCommandsChannel;
        this.jda = jdaEvent.getJDA();
        this.channel = jdaEvent.getChannel();
        this.message = jdaEvent.getMessage();
        this.author = jdaEvent.getAuthor();
        this.guild = jdaEvent.getGuild();
        this.member = jdaEvent.getMember();
        //I've copied all the most important properties from the GuildMessageRecievedEvent over just in case anyone using this event ever needs them
    }
}
