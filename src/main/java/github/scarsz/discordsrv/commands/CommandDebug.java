package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.util.DebugUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class CommandDebug {

    @Command(commandNames = { "debug" },
            helpMessage = "Dumps DiscordSRV debug information to GitHub Gists or the plugin folder",
            permission = "discordsrv.debug"
    )
    public static void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.AQUA + DebugUtil.run(sender instanceof ConsoleCommandSender ? "CONSOLE" : sender.getName()));
    }

}
