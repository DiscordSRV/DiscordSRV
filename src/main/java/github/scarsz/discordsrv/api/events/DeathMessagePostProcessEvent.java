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

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * <p>Called after DiscordSRV has processed a death message but before being sent to Discord.
 * Modification is allow and will effect the message sent to Discord.</p>
 */
@SuppressWarnings({"LombokGetterMayBeUsed", "LombokSetterMayBeUsed"})
public class DeathMessagePostProcessEvent extends GameEvent<PlayerDeathEvent> implements Cancellable {

    private boolean cancelled;

    private final String deathMessage;
    private String channel;

    private Message discordMessage;
    private boolean usingWebhooks;
    private String webhookName;
    private String webhookAvatarUrl;

    public DeathMessagePostProcessEvent(String channel, Message discordMessage, Player player, String deathMessage, PlayerDeathEvent triggeringBukkitEvent, boolean usingWebhooks, String webhookName, String webhookAvatarUrl, boolean cancelled) {
        super(player, triggeringBukkitEvent);
        this.channel = channel;
        this.discordMessage = discordMessage;
        this.deathMessage = deathMessage;
        this.usingWebhooks = usingWebhooks;
        this.webhookName = webhookName;
        this.webhookAvatarUrl = webhookAvatarUrl;
        setCancelled(cancelled);
    }

    @Deprecated
    public DeathMessagePostProcessEvent(String channel, Message discordMessage, Player player, String deathMessage, boolean usingWebhooks, String webhookName, String webhookAvatarUrl, boolean cancelled) {
        super(player, null);
        this.channel = channel;
        this.discordMessage = discordMessage;
        this.deathMessage = deathMessage;
        this.usingWebhooks = usingWebhooks;
        this.webhookName = webhookName;
        this.webhookAvatarUrl = webhookAvatarUrl;
        setCancelled(cancelled);
    }

    @Deprecated
    public DeathMessagePostProcessEvent(String channel, String processedMessage, Player player, String deathMessage, boolean cancelled) {
        super(player, null);
        this.channel = channel;
        this.discordMessage = new MessageBuilder().setContent(processedMessage).build();
        this.deathMessage = deathMessage;
        setCancelled(cancelled);
    }

    @Deprecated
    public String getProcessedMessage() {
        return discordMessage.getContentRaw();
    }

    @Deprecated
    public void setProcessedMessage(String processedMessage) {
        this.discordMessage = new MessageBuilder(processedMessage).build();
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public String getDeathMessage() {
        return this.deathMessage;
    }

    public String getChannel() {
        return this.channel;
    }

    public Message getDiscordMessage() {
        return this.discordMessage;
    }

    public boolean isUsingWebhooks() {
        return this.usingWebhooks;
    }

    public String getWebhookName() {
        return this.webhookName;
    }

    public String getWebhookAvatarUrl() {
        return this.webhookAvatarUrl;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setDiscordMessage(Message discordMessage) {
        this.discordMessage = discordMessage;
    }

    public void setUsingWebhooks(boolean usingWebhooks) {
        this.usingWebhooks = usingWebhooks;
    }

    public void setWebhookName(String webhookName) {
        this.webhookName = webhookName;
    }

    public void setWebhookAvatarUrl(String webhookAvatarUrl) {
        this.webhookAvatarUrl = webhookAvatarUrl;
    }
}
