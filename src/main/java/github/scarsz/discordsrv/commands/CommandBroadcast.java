package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandBroadcast {

    @Command(commandNames = { "broadcast", "bcast" },
            helpMessage = "Broadcasts a message to the main text channel on Discord",
            permission = "discordsrv.bcast",
            usageExample = "broadcast Hello from the server!"
    )
    public static void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.NO_MESSAGE_GIVEN_TO_BROADCAST.toString());
        } else {
            DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), String.join(" ", args));
        }
    }

}
