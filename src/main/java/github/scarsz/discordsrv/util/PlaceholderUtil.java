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
import github.scarsz.discordsrv.hooks.VaultHook;
import github.scarsz.discordsrv.objects.Lag;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// A bunch of general placeholders.
public class PlaceholderUtil {
    // Tries to apply placeholderAPI placeholders
    public static String applyPlaceholderApi(Player player, String text) {
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        else return text;
    }

    // General placeholders for a online player
    public static String applyPlayerPlaceholders(Player player, String text, boolean escapeMarkdown) {
        if (player != null)
            return text
                .replace("%minecraft_playername", notNull(escapeMarkdown ? DiscordUtil.escapeMarkdown(player.getName()) : player.getName()))
                .replace("%minecraft_displayname", notNull(escapeMarkdown ? DiscordUtil.escapeMarkdown(player.getDisplayName()) : player.getDisplayName()))
                .replace("%minecraft_uuid%", notNull(player.getUniqueId().toString()))
                .replace("%minecraft_group", notNull(VaultHook.getPrimaryGroup(player)));
        else return text;
    }

    public static String applyPlayerPlaceholders(Player player, String text) {
        return applyPlayerPlaceholders(player, text, false);
    }

    // General placeholders for the online server
    public static String applyOnlineServerPlaceholders(String text) {
        final Map<String, String> mem = MemUtil.get();
        return text
                .replace("%server_playercount%", notNull(Integer.toString(PlayerUtil.getOnlinePlayers(true).size())))
                .replace("%server_playermax%", notNull(Integer.toString(Bukkit.getMaxPlayers())))
                .replace("%server_totalplayers%", notNull(Integer.toString(DiscordSRV.getTotalPlayerCount())))
                .replace("%server_uptimemins%", notNull(Long.toString(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                .replace("%server_uptimehours%", notNull(Long.toString(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                .replace("%server_uptimedays%", notNull(Long.toString(TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                .replace("%server_motd%", notNull(StringUtils.isNotBlank(Bukkit.getMotd()) ? DiscordUtil.strip(Bukkit.getMotd()) : ""))
                .replace("%server_serverversion%", notNull(Bukkit.getBukkitVersion()))
                .replace("%server_freememory%", notNull(mem.get("freeMB")))
                .replace("%server_usedmemory%", notNull(mem.get("usedMB")))
                .replace("%server_totalmemory%", notNull(mem.get("totalMB")))
                .replace("%server_maxmemory%", notNull(mem.get("maxMB")))
                .replace("%server_freememorygb%", notNull(mem.get("freeGB")))
                .replace("%server_usedmemorygb%", notNull(mem.get("usedGB")))
                .replace("%server_totalmemorygb%", notNull(mem.get("totalGB")))
                .replace("%server_maxmemorygb%", notNull(mem.get("maxGB")))
                .replace("%server_tps%", notNull(Lag.getTPSString()));
    }

    // General placeholders for a Discord User
    public static String applyUserPlaceholders(User user, String text) {
        if (user != null) {
            text = text
                .replace("%discord_name%", notNull(user.getName()))
                .replace("%discord_tag%", notNull(user.getAsTag()))
                .replace("%discord_mention%", notNull(user.getAsMention()))
                .replace("%discord_id%", notNull(user.getId()));
            Member member = DiscordSRV.getPlugin().getMainGuild().getMember(user);
            if (member != null) {
                List<Role> roles = member.getRoles();
                text = text
                        .replace("%discord_nickname%", notNull(member.getEffectiveName()))
                        .replace("%discord_toprole%", notNull(DiscordUtil.getRoleName(!roles.isEmpty() ? roles.get(0) : null)))
                        .replace("%discord_toproleinitial%", notNull(!roles.isEmpty() ? DiscordUtil.getRoleName(roles.get(0)).substring(0, 1) : ""))
                        .replace("%discord_allroles%", notNull(DiscordUtil.getFormattedRoles(roles)));
            }
        }
        return text;
    }

    // General placeholders for the main guild
    public static String applyGuildPlaceholders(String text) {
        Guild guild = DiscordSRV.getPlugin().getMainGuild();
        return text
                .replace("%guild_name%", notNull(guild.getName()))
                .replace("%guild_members%", notNull(guild.getMembers().size()));
    }

    // Placeholders to apply mentions for roles, channels, and users for Discord
    public static String applyMentions(String text) {
        String[] split = text.split("%");
        if (split.length == 1)
            return text;

        for (String string : split) {
            if (string.startsWith("mention_role")) {
                String id = string.substring("mention_role_".length());
                text = text.replace("%mention_role_" + id + "%", "<@&" + id + ">");
            }
            if (string.startsWith("mention_user")) {
                String id = string.substring("mention_user_".length());
                text = text.replace("%mention_user_" + id + "%", "<@" + id + ">");
            }
            if (string.startsWith("mention_channel")) {
                String id = string.substring("mention_channel_".length());
                text = text.replace("%mention_channel_" + id + "%", "<#" + id + ">");
            }
        }
        text = text.replace("%mention_guildowner%", DiscordSRV.getPlugin().getMainGuild().getOwner().getAsMention());
        return text;
    }

    public static String applyAll(Player player, User user, String text, boolean escapeMarkdown) {
        text = applyPlayerPlaceholders(player, text, escapeMarkdown);
        text = applyOnlineServerPlaceholders(text);
        text = applyUserPlaceholders(user, text);
        text = applyGuildPlaceholders(text);
        text = applyPlaceholderApi(player, text);
        return text;
    }

    public static String applyAll(Player player, User user, String text) {
        return applyAll(player, user, text, false);
    }

    public static String applyLegacy(String legacyPlaceholder, String data, String text, String newPlaceholder) {
        if (text.contains(legacyPlaceholder)) {
            DiscordSRV.info("Found legacy placeholder " + legacyPlaceholder + " in " + text + ". This should be replaced with " + newPlaceholder);
            text = text.replace(legacyPlaceholder, data);
        }
        return text;
    }

    private static String notNull(Object object) {
        return object != null ? object.toString() : "";
    }
}
