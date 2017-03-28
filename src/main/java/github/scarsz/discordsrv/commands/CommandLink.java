package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 3:06 PM
 */
public class CommandLink {

    @Command(commandNames = { "link" },
            helpMessage = "Generates a code to link your Minecraft account to your Discord account",
            permission = "discordsrv.link"
    )
    public static void execute(Player sender, String[] args) {
        String code = DiscordSRV.getPlugin().getAccountLinkManager().generateCode(sender.getUniqueId());

        sender.sendMessage(ChatColor.AQUA + "Your link code is " + code + ". Send a private message to the bot (" + DiscordSRV.getPlugin().getMainGuild().getMember(DiscordUtil.getJda().getSelfUser()).getEffectiveName() + ") on Discord with just this code as the message to link your Discord account to your UUID.");
    }

}
