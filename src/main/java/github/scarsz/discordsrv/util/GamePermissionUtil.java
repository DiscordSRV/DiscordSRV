package github.scarsz.discordsrv.util;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.Permissible;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 2:22 PM
 */
public class GamePermissionUtil {

    public static boolean hasPermission(Permissible sender, String permission) {
        return sender instanceof ConsoleCommandSender || sender.hasPermission(permission);
    }

}
