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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Achievement;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PrettyUtil {

    public static String beautify(User user) {
        if (user == null) return "<Unknown>";

        Member member = DiscordSRV.getPlugin().getMainGuild().getMember(user);

        return member != null
                ? member.getEffectiveName() + " (#" + user.getId() + ")"
                : user.getName() + " (#" + user.getId() + ")";
    }

    public static String beautify(OfflinePlayer player) {
        if (player == null || player.getName() == null) return "<Unknown>";

        return player.isOnline()
                ? DiscordUtil.strip(player.getPlayer().getDisplayName()) + " (" + player.getUniqueId() + ")"
                : player.getName() + " (" + player.getUniqueId() + ")";
    }

    /**
     * turn "ACHIEVEMENT_NAME" into "Achievement Name"
     * @param achievement achievement to beautify
     * @return pretty achievement name
     */
    @SuppressWarnings("deprecation")
    public static String beautify(Achievement achievement) {
        if (achievement == null) return "<✗>";

        return Arrays.stream(achievement.name().toLowerCase().split("_"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    public static String beautify(StackTraceElement[] stackTraceElements) {
        return Arrays.stream(stackTraceElements).map(stackTraceElement -> "\t" + stackTraceElement.toString()).skip(1).collect(Collectors.joining("\n"));
    }

}
