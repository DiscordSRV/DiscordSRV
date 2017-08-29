package github.scarsz.discordsrv.util;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.Permissible;

public class GamePermissionUtil {

    public static boolean hasPermission(Permissible sender, String permission) {
        return sender instanceof ConsoleCommandSender || sender.hasPermission(permission);
    }

}
