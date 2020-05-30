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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.AchievementMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.AchievementMessagePreProcessEvent;
import github.scarsz.discordsrv.hooks.world.MultiverseCoreHook;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAchievementAwardedEvent;

@SuppressWarnings("deprecation")
public class PlayerAchievementsListener implements Listener {

    public PlayerAchievementsListener() {
        if (PlayerAchievementAwardedEvent.class.isAnnotationPresent(Deprecated.class)) return;

        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAchievementAwarded(PlayerAchievementAwardedEvent event) {
        // return if achievement messages are disabled
        if (StringUtils.isBlank(LangUtil.Message.PLAYER_ACHIEVEMENT.toString())) return;

        // return if achievement or player objects are knackered because this can apparently happen for some reason
        if (event == null || event.getAchievement() == null || event.getPlayer() == null) return;

        // respect invisibility plugins
        if (PlayerUtil.isVanished(event.getPlayer())) return;

        // turn "ACHIEVEMENT_NAME" into "Achievement Name"
        String channelName = DiscordSRV.getPlugin().getMainChatChannel();
        String achievementName = PrettyUtil.beautify(event.getAchievement());
        String message = LangUtil.Message.PLAYER_ACHIEVEMENT.toString();
        Player player = event.getPlayer();

        AchievementMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new AchievementMessagePreProcessEvent(channelName, message, player, achievementName));
        if (preEvent.isCancelled()) {
            DiscordSRV.debug("AchievementMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }
        // Update from event in case any listeners modified parameters
        achievementName = preEvent.getAchievementName();
        channelName = preEvent.getChannel();
        message = preEvent.getMessage();

        String discordMessage = message
                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%username%", player.getName())
                .replace("%displayname%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(player.getDisplayName())))
                .replace("%world%", player.getWorld().getName())
                .replace("%worldalias%", DiscordUtil.strip(MultiverseCoreHook.getWorldAlias(player.getWorld().getName())))
                .replace("%achievement%", achievementName);
        discordMessage = PlaceholderUtil.replacePlaceholdersToDiscord(discordMessage, player);

        AchievementMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new AchievementMessagePostProcessEvent(channelName, discordMessage, player, achievementName, preEvent.isCancelled()));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug("AchievementMessagePostProcessEvent was cancelled, message send aborted");
            return;
        }
        // Update from event in case any listeners modified parameters
        channelName = postEvent.getChannel();
        discordMessage = postEvent.getProcessedMessage();

        TextChannel channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);

        DiscordUtil.sendMessage(channel, discordMessage);
    }

}
