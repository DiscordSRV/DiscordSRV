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

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.Lag;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PlaceholderUtil {

    private PlaceholderUtil() {}

    public static String replacePlaceholders(String input) {
        return replacePlaceholders(input, null);
    }

    public static String replacePlaceholders(String input, OfflinePlayer player) {
        if (input == null) return null;
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) {
            Player onlinePlayer = player != null ? player.getPlayer() : null;
            input = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(
                    onlinePlayer != null ? onlinePlayer : player, input);
        }
        return input;
    }

    /**
     * Important when the content may contain role mentions
     */
    public static String replacePlaceholdersToDiscord(String input) {
        return replacePlaceholdersToDiscord(input, null);
    }

    /**
     * Important when the content may contain role mentions
     */
    public static String replacePlaceholdersToDiscord(String input, OfflinePlayer player) {
        boolean placeholderapi = PluginUtil.pluginHookIsEnabled("placeholderapi");

        // PlaceholderAPI has a side effect of replacing chat colors at the end of placeholder conversion
        // that breaks role mentions: <@&role id> because it converts the & to a §
        // So we add a zero width space after the & to prevent it from translating, and remove it after conversion
        if (placeholderapi) input = input.replace("&", "&\u200B");

        input = replacePlaceholders(input, player);

        if (placeholderapi) {
            input = MessageUtil.stripLegacy(input); // PAPI no longer replaces chat colors? strip both legacy codes
            input = input.replace("&\u200B", "&");
        }
        return input;
    }

    /*
     * Placeholders for the channel topic updater & channel updater
     */
    @SuppressWarnings({"SpellCheckingInspection"})
    public static String replaceChannelUpdaterPlaceholders(String input) {
        if (StringUtils.isBlank(input)) return "";

        // set PAPI placeholders
        input = PlaceholderUtil.replacePlaceholdersToDiscord(input);

        final Map<String, String> mem = MemUtil.get();

        input = input.replaceAll("%time%|%date%", notNull(TimeUtil.timeStamp()))
                .replace("%playercount%", notNull(Integer.toString(PlayerUtil.getOnlinePlayers(true).size())))
                .replace("%playermax%", notNull(Integer.toString(Bukkit.getMaxPlayers())))
                .replace("%totalplayers%", notNull(Integer.toString(DiscordSRV.getTotalPlayerCount())))
                .replace("%uptimemins%", notNull(Long.toString(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                .replace("%uptimehours%", notNull(Long.toString(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                .replace("%uptimedays%", notNull(Long.toString(TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                .replace("%timestamp%", notNull(Long.toString(System.currentTimeMillis() / 1000)))
                .replace("%starttimestamp%", notNull(Long.toString(TimeUnit.MILLISECONDS.toSeconds(DiscordSRV.getPlugin().getStartTime()))))
                .replace("%motd%", notNull(StringUtils.isNotBlank(Bukkit.getMotd()) ? MessageUtil.strip(Bukkit.getMotd()) : ""))
                .replace("%serverversion%", notNull(Bukkit.getBukkitVersion()))
                .replace("%freememory%", notNull(mem.get("freeMB")))
                .replace("%usedmemory%", notNull(mem.get("usedMB")))
                .replace("%totalmemory%", notNull(mem.get("totalMB")))
                .replace("%maxmemory%", notNull(mem.get("maxMB")))
                .replace("%freememorygb%", notNull(mem.get("freeGB")))
                .replace("%usedmemorygb%", notNull(mem.get("usedGB")))
                .replace("%totalmemorygb%", notNull(mem.get("totalGB")))
                .replace("%maxmemorygb%", notNull(mem.get("maxGB")))
                .replace("%tps%", notNull(Lag.getTPSString()));

        return input;
    }

    public static String notNull(Object object) {
        return object != null ? object.toString() : "";
    }

}
