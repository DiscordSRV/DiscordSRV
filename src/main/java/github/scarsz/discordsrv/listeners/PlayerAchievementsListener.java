/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
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
import github.scarsz.discordsrv.hooks.world.MultiverseCoreHook;
import github.scarsz.discordsrv.util.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
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

        // return if achievement or player objects are fucking knackered because this can apparently happen for some reason
        if (event == null || event.getAchievement() == null || event.getPlayer() == null) return;

        // turn "SHITTY_ACHIEVEMENT_NAME" into "Shitty Achievement Name"
        String achievementName = PrettyUtil.beautify(event.getAchievement());

        String discordMessage = LangUtil.Message.PLAYER_ACHIEVEMENT.toString()
                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%username%", event.getPlayer().getName())
                .replace("%displayname%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(event.getPlayer().getDisplayName())))
                .replace("%world%", event.getPlayer().getWorld().getName())
                .replace("%worldalias%", DiscordUtil.strip(MultiverseCoreHook.getWorldAlias(event.getPlayer().getWorld().getName())))
                .replace("%achievement%", achievementName);
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) discordMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(event.getPlayer(), discordMessage);

        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), discordMessage);
    }

}
