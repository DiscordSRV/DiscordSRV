/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
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

import github.scarsz.discordsrv.objects.MessageFormat;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class AchievementMessagePostProcessEvent extends GameEvent implements Cancellable {

    @Getter @Setter private boolean cancelled;

    @Getter private String achievementName;
    @Getter @Setter private String channel;

    @Getter @Setter private Message discordMessage;
    @Getter @Setter private boolean usingWebhooks;
    @Getter @Setter private String webhookName;
    @Getter @Setter private String webhookAvatarUrl;

    public AchievementMessagePostProcessEvent(String channel, Message discordMessage, Player player, String achievementName, boolean usingWebhooks, String webhookName, String webhookAvatarUrl, boolean cancelled) {
        super(player);
        this.channel = channel;
        this.discordMessage = discordMessage;
        this.achievementName = achievementName;
        this.usingWebhooks = usingWebhooks;
        this.webhookName = webhookName;
        this.webhookAvatarUrl = webhookAvatarUrl;
        setCancelled(cancelled);
    }

    @Deprecated
    public AchievementMessagePostProcessEvent(String channel, String processedMessage, Player player, String achievementName, boolean cancelled) {
        super(player);
        this.channel = channel;
        this.discordMessage = new MessageBuilder().setContent(processedMessage).build();
        this.achievementName = achievementName;
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

}
