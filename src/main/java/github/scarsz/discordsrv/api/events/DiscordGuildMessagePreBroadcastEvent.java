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

import net.dv8tion.jda.api.entities.User;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

import java.util.List;


/**
 * <p>Called before DiscordSRV broadcasts a message to a chat channel on the Minecraft server.</p>
 *
 * <p>Depending on the type of chat plugin used, {@link #getRecipients()} may return an immutable list.
 * Using {@link #isRecipientsMutable()} can be used to check if the list is mutable, and recipients can
 * be added or removed.</p>
 *
 * <p>Currently, only TownyChat and VentureChat support mutable recipient lists.</p>
 *
 * <p>At the time this event is called, the message, channel, and recipients can still be changed</p>
 */
@SuppressWarnings({"LombokGetterMayBeUsed", "LombokSetterMayBeUsed"})
public class DiscordGuildMessagePreBroadcastEvent extends Event {

    private String channel;
    private Component message;
    private final User author;
    private final List<? extends CommandSender> recipients;

    public DiscordGuildMessagePreBroadcastEvent(String channel, Component message, User author, List<? extends CommandSender> recipients) {
        this.channel = channel;
        this.message = message;
        this.author = author;
        this.recipients = recipients;
    }

    public String getChannel() {
        return this.channel;
    }

    public Component getMessage() {
        return this.message;
    }

    public User getAuthor() {
        return author;
    }

    public List<? extends CommandSender> getRecipients() {
        return this.recipients;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setMessage(Component message) {
        this.message = message;
    }

    public boolean isRecipientsMutable() {
        // You can really only check if a list is mutable
        // in java by trying to add or remove an element.
        try {
            recipients.add(null);
            recipients.remove(null);
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }
}
