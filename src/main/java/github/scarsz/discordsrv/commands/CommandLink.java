package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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

        if (DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId()) != null) {
            sender.sendMessage(ChatColor.AQUA + LangUtil.InternalMessage.ACCOUNT_ALREADY_LINKED.toString());
        } else {
            String code = DiscordSRV.getPlugin().getAccountLinkManager().generateCode(sender.getUniqueId());

            sender.sendMessage(ChatColor.AQUA + LangUtil.Message.CODE_GENERATED.toString()
                    .replace("%code%", code)
                    .replace("%botname%", DiscordSRV.getPlugin().getMainGuild().getMember(DiscordUtil.getJda().getSelfUser()).getEffectiveName())
            );
        }
    }

}
