/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.api.events;

import github.scarsz.discordsrv.api.Cancellable;
import github.scarsz.discordsrv.util.MessageUtil;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.kyori.adventure.text.Component;

/**
 * <p>Called directly after a message from Discord was processed but before being broadcasted to the Minecraft server</p>
 *
 * <p>At the time this event is called, {@link #getProcessedMessage()} would return what the final message
 * would look like in-game, including text like the author before the actual message to which you could use
 * {@link #setMinecraftMessage(Component)} to change the message that would be broadcasted in-game or
 * {@link #setCancelled(boolean)} to cancel it from being broadcasted altogether</p>
 */
@SuppressWarnings({"LombokGetterMayBeUsed", "LombokSetterMayBeUsed"})
public class DiscordGuildMessagePostProcessEvent extends DiscordEvent<GuildMessageReceivedEvent> implements Cancellable {

    private boolean cancelled;

    private final User author;
    private final TextChannel channel;
    private final Guild guild;
    private final Member member;
    private final Message message;

    private Component minecraftMessage;

    @Deprecated
    public DiscordGuildMessagePostProcessEvent(GuildMessageReceivedEvent jdaEvent, boolean cancelled, String processedMessage) {
        super(jdaEvent.getJDA(), jdaEvent);
        this.author = jdaEvent.getAuthor();
        this.channel = jdaEvent.getChannel();
        this.guild = jdaEvent.getGuild();
        this.member = jdaEvent.getMember();
        this.message = jdaEvent.getMessage();

        this.setCancelled(cancelled);
        this.setProcessedMessage(processedMessage);
    }

    public DiscordGuildMessagePostProcessEvent(GuildMessageReceivedEvent jdaEvent, boolean cancelled, Component minecraftMessage) {
        super(jdaEvent.getJDA(), jdaEvent);
        this.author = jdaEvent.getAuthor();
        this.channel = jdaEvent.getChannel();
        this.guild = jdaEvent.getGuild();
        this.member = jdaEvent.getMember();
        this.message = jdaEvent.getMessage();

        this.setCancelled(cancelled);
        this.minecraftMessage = minecraftMessage;
    }

    @Deprecated
    public void setProcessedMessage(String processedMessage) {
        this.minecraftMessage = MessageUtil.toComponent(processedMessage);
    }

    @Deprecated
    public String getProcessedMessage() {
        return MessageUtil.toLegacy(minecraftMessage);
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public User getAuthor() {
        return this.author;
    }

    public TextChannel getChannel() {
        return this.channel;
    }

    public Guild getGuild() {
        return this.guild;
    }

    public Member getMember() {
        return this.member;
    }

    public Message getMessage() {
        return this.message;
    }

    /**
     * The message that will be sent to players in-game
     */
    public Component getMinecraftMessage() {
        return this.minecraftMessage;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setMinecraftMessage(Component minecraftMessage) {
        this.minecraftMessage = minecraftMessage;
    }
}
