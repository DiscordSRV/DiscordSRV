package github.scarsz.discordsrv.util;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 2:22 PM
 */
public class GamePermissionUtil {

    public static boolean hasPermission(CommandSender sender, String permission) {
        return sender instanceof ConsoleCommandSender || hasPermission((Player) sender, permission);
    }

    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission) || player.hasPermission("discordsrv.admin");
    }

}
