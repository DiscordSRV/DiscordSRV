package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 3:06 PM
 */
public class CommandUnsubscribe {

    @Command(commandNames = { "unsubscribe" },
            helpMessage = "Unsubscribes yourself from Discord messages",
            permission = "discordsrv.unsubscribe"
    )
    public static void execute(Player sender, String[] args) {
        DiscordSRV.getPlugin().setIsSubscribed(sender.getUniqueId(), false);

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', LangUtil.Message.ON_UNSUBSCRIBE.toString()));
    }

}
