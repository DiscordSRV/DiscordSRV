package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.bukkit.Achievement;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PrettyUtil {

    public static String beautify(User user) {
        if (user == null) return "<✗>";

        Member member = DiscordSRV.getPlugin().getMainGuild().getMember(user);

        return member != null
                ? member.getEffectiveName() + " (#" + user.getId() + ")"
                : user.getName() + " (#" + user.getId() + ")";
    }

    public static String beautify(OfflinePlayer player) {
        if (player == null) return "<✗>";

        return player.isOnline()
                ? DiscordUtil.strip(player.getPlayer().getDisplayName()) + " (" + player.getUniqueId() + ")"
                : player.getName() + " (" + player.getUniqueId() + ")";
    }

    /**
     * turn "SHITTY_ACHIEVEMENT_NAME" into "Shitty Achievement Name"
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
