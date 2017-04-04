package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
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
public class CommandLink {

    @Command(commandNames = { "link" },
            helpMessage = "Generates a code to link your Minecraft account to your Discord account",
            permission = "discordsrv.link"
    )
    public static void execute(Player sender, String[] args) {
        if (DiscordSRV.getPlugin().getAccountLinkManager() == null) {
            sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.UNABLE_TO_LINK_ACCOUNTS_RIGHT_NOW.toString());
            return;
        }

        String code = DiscordSRV.getPlugin().getAccountLinkManager().generateCode(sender.getUniqueId());

        sender.sendMessage(ChatColor.AQUA + LangUtil.InternalMessage.LINK_CODE_GENERATED.toString()
                .replace("{code}", code)
                .replace("{botname}", DiscordSRV.getPlugin().getMainGuild().getMember(DiscordUtil.getJda().getSelfUser()).getEffectiveName())
        );
    }

}
