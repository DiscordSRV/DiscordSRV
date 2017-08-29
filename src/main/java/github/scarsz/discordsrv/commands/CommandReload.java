package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandReload {

    @Command(commandNames = { "reload" },
            helpMessage = "Reloads the config of DiscordSRV",
            permission = "discordsrv.reload"
    )
    public static void execute(CommandSender sender, String[] args) {
        DiscordSRV.getPlugin().reloadConfig();
        LangUtil.reloadMessages();

        sender.sendMessage(ChatColor.AQUA + LangUtil.InternalMessage.RELOADED.toString());
    }

}
