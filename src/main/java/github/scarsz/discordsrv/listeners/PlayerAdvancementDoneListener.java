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
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PlayerAdvancementDoneListener implements Listener {

    public PlayerAdvancementDoneListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        // return if advancement messages are disabled
        if (StringUtils.isBlank(LangUtil.Message.PLAYER_ACHIEVEMENT.toString())) return;

        // return if advancement or player objects are knackered because this can apparently happen for some reason
        if (event.getAdvancement() == null || event.getAdvancement().getKey().getKey().contains("recipe/") || event.getPlayer() == null) return;

        // respect invisibility plugins
        if (PlayerUtil.isVanished(event.getPlayer())) return;

        try {
            Object craftAdvancement = ((Object) event.getAdvancement()).getClass().getMethod("getHandle").invoke(event.getAdvancement());
            Object advancementDisplay = craftAdvancement.getClass().getMethod("c").invoke(craftAdvancement);
            boolean display = (boolean) advancementDisplay.getClass().getMethod("i").invoke(advancementDisplay);
            if (!display) return;
        } catch (NullPointerException e) {
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String channelName = DiscordSRV.getPlugin().getMainChatChannel();
        String message = LangUtil.Message.PLAYER_ACHIEVEMENT.toString();
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();

        // turn "story/advancement_name" into "Advancement Name"
        String rawAdvancementName = advancement.getKey().getKey();
        String advancementName = Arrays.stream(rawAdvancementName.substring(rawAdvancementName.lastIndexOf("/") + 1).toLowerCase().split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(" "));

        AchievementMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new AchievementMessagePreProcessEvent(channelName, message, player, advancementName));
        if (preEvent.isCancelled()) {
            DiscordSRV.debug("AchievementMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }
        // Update from event in case any listeners modified parameters
        advancementName = preEvent.getAchievementName();
        channelName = preEvent.getChannel();
        message = preEvent.getMessage();

        String discordMessage = message
                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%username%", player.getName())
                .replace("%displayname%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(player.getDisplayName())))
                .replace("%world%", player.getWorld().getName())
                .replace("%achievement%", advancementName);
        discordMessage = PlaceholderUtil.replacePlaceholdersToDiscord(discordMessage, event.getPlayer());

        AchievementMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new AchievementMessagePostProcessEvent(channelName, discordMessage, player, advancementName, preEvent.isCancelled()));
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
