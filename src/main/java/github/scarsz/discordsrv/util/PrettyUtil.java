/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
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
 * END
 */

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PrettyUtil {

    public static String beautify(User user) {
        return beautify(user, "<Unknown>", true);
    }

    public static String beautify(User user, String noUsernameFormat, boolean includeId) {
        if (user == null) return noUsernameFormat;

        Member member = DiscordSRV.getPlugin().getMainGuild().getMember(user);

        return member != null
                ? member.getEffectiveName() + (includeId ? " (#" + user.getId() + ")" : "")
                : user.getName() + (includeId ? " (#" + user.getId() + ")" : "");
    }

    public static String beautifyUsername(OfflinePlayer player) {
        return beautifyUsername(player, "<Unknown>", true);
    }

    public static String beautifyUsername(OfflinePlayer player, String noUsernameFormat, boolean includeUuid) {
        if (player == null) return noUsernameFormat;

        String name = player.getName();
        if (name == null && player.isOnline()) {
            // maybe this will work?
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer != null) {
                name = onlinePlayer.getName();
            }
        }
        return (name != null ? name : noUsernameFormat) + (includeUuid ? " (" + player.getUniqueId() + ")" : "");
    }

    /**
     * Turns a {@link OfflinePlayer} into Nickname/Username (UUID)
     * @param player the offline player
     * @return the player's nickname (if online) or username (if offline) and the UUID or if player is null "<Unknown>"
     */
    public static String beautifyNickname(OfflinePlayer player) {
        return beautifyNickname(player, "<Unknown>", true);
    }

    public static String beautifyNickname(OfflinePlayer player, String noUsernameFormat, boolean includeUuid) {
        if (player == null || player.getName() == null) return noUsernameFormat;

        if (player.isOnline()) {
            if (player.getPlayer() == null) return beautifyUsername(player);
            String displayName = player.getPlayer().getDisplayName();
            if (StringUtils.isBlank(displayName)) return beautifyUsername(player);
            return MessageUtil.strip(displayName) + (includeUuid ? " (" + player.getUniqueId() + ")" : "");
        } else {
            return beautifyUsername(player);
        }
    }

    /**
     * turn "ACHIEVEMENT_NAME" into "Achievement Name"
     * @param achievement achievement to beautify
     * @return pretty achievement name
     */
    public static String beautify(Enum<?> achievement) {
        if (achievement == null) return "<✗>";

        return Arrays.stream(achievement.name().toLowerCase().split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    public static String beautify(StackTraceElement[] stackTraceElements) {
        return Arrays.stream(stackTraceElements).map(stackTraceElement -> "\t" + stackTraceElement.toString()).skip(1).collect(Collectors.joining("\n"));
    }

}
