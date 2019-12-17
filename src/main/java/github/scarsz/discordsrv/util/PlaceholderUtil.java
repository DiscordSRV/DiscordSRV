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

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.Lag;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.TimeUnit;

// A bunch of general placeholders. All discordSRV internal placeholders are prefixed with "dsrv_" to prevent placeholderapi conflicts
public class PlaceholderUtil {
    // Tries to apply placeholderAPI placeholders
    public static String applyPlaceholderApi(Player player, String text) {
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        else return text;
    }

    // General placeholders for a online player
    public static String applyPlayerPlaceholders(Player player, String text) {
        return text
                .replace("%dsrv_minecraft_playername", notNull(player.getName()))
                .replace("%dsrv_minecraft_displayname", notNull(player.getDisplayName()))
                .replace("%dsrv_minecraft_uuid%", notNull(player.getUniqueId().toString()));
    }

    // General placeholders for the online server
    public static String applyOnlineServerPlaceholders(String text) {
        final Map<String, String> mem = MemUtil.get();
        return text
                .replace("%dsrv_server_playermax%", notNull(Integer.toString(Bukkit.getMaxPlayers())))
                .replace("%dsrv_server_totalplayers%", notNull(Integer.toString(DiscordSRV.getTotalPlayerCount())))
                .replace("%dsrv_server_uptimemins%", notNull(Long.toString(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                .replace("%dsrv_server_uptimehours%", notNull(Long.toString(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                .replace("%dsrv_server_uptimedays%", notNull(Long.toString(TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                .replace("%dsrv_server_motd%", notNull(StringUtils.isNotBlank(Bukkit.getMotd()) ? DiscordUtil.strip(Bukkit.getMotd()) : ""))
                .replace("%dsrv_server_serverversion%", notNull(Bukkit.getBukkitVersion()))
                .replace("%dsrv_server_freememory%", notNull(mem.get("freeMB")))
                .replace("%dsrv_server_usedmemory%", notNull(mem.get("usedMB")))
                .replace("%dsrv_server_totalmemory%", notNull(mem.get("totalMB")))
                .replace("%dsrv_server_maxmemory%", notNull(mem.get("maxMB")))
                .replace("%dsrv_server_freememorygb%", notNull(mem.get("freeGB")))
                .replace("%dsrv_server_usedmemorygb%", notNull(mem.get("usedGB")))
                .replace("%dsrv_server_totalmemorygb%", notNull(mem.get("totalGB")))
                .replace("%dsrv_server_maxmemorygb%", notNull(mem.get("maxGB")))
                .replace("%dsrv_server_tps%", notNull(Lag.getTPSString()));
    }

    // General placeholders for a Discord User
    public static String applyUserPlaceholders(User user, String text) {
        return text
                .replace("dsrv_discord_name", notNull(user.getName()))
                .replace("dsrv_discord_tag", notNull(user.getAsTag()))
                .replace("dsrv_discord_mention", notNull(user.getAsMention()));
    }

    // General placeholders for the main guild
    public static String applyGuildPlaceholders(String text) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        return text
                .replace("dsrv_guild_name", notNull(guild.getName()))
                .replace("dsrv_guild_members", notNull(guild.getMembers().size()));
    }

    private static String notNull(Object object) {
        return object != null ? object.toString() : "";
    }
}
