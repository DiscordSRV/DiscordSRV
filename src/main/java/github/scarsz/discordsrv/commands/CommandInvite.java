package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.api.Permission;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandInvite {
    @Command(commandNames = { "invite" },
            helpMessage = "Generates an invite code for the bot",
            permission = "discordsrv.invite"
    )
    public static void execute(CommandSender sender, String[] args) {
        String invite = DiscordUtil.getJda().getInviteUrl(Permission.ADMINISTRATOR);
        sender.sendMessage(ChatColor.DARK_AQUA + "You may invite the bot with this link:\n" + ChatColor.AQUA + invite);
    }
}
