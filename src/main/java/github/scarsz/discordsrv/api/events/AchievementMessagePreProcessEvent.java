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
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class AchievementMessagePreProcessEvent extends GameEvent implements Cancellable {

    @Getter @Setter private boolean cancelled;

    @Getter @Setter private String achievementName;
    @Getter @Setter private String channel;
    @Getter @Setter private MessageFormat messageFormat;

    public AchievementMessagePreProcessEvent(String channel, MessageFormat messageFormat, Player player, String achievementName) {
        super(player);
        this.channel = channel;
        this.messageFormat = messageFormat;
        this.achievementName = achievementName;
    }

    @Deprecated
    public AchievementMessagePreProcessEvent(String channel, String message, Player player, String achievementName) {
        super(player);
        this.channel = channel;
        MessageFormat messageFormat = new MessageFormat();
        messageFormat.setContent(message);
        this.messageFormat = messageFormat;
        this.achievementName = achievementName;
    }

    @Deprecated
    public String getMessage() {
        return messageFormat.getContent();
    }

    @Deprecated
    public void setMessage(String message) {
        MessageFormat messageFormat = new MessageFormat();
        messageFormat.setContent(message);
        this.messageFormat = messageFormat;
    }

}
