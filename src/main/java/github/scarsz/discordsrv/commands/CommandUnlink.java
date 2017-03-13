package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 3:06 PM
 */
public class CommandUnlink {

    @Command(commandNames = { "unlink", "clearlinked" },
            helpMessage = "Unlinks your Minecraft account from your Discord account",
            permission = "discordsrv.unlink"
    )
    public static void execute(Player sender, String[] args) {
        DiscordSRV.getPlugin().getAccountLinkManager().unlink(sender.getUniqueId());

        sender.sendMessage(ChatColor.AQUA + "Your UUID is no longer associated with " + (DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId()) != null
                ? DiscordUtil.getJda().getUserById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId())) != null
                    ? DiscordUtil.getJda().getUserById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId()))
                    : DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId())
                : "anybody. It never was."));
    }

}
