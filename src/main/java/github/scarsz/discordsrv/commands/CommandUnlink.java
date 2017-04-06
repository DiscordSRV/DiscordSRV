package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import net.dv8tion.jda.core.entities.Member;
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

        String linkedId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId());
        boolean hadLinkedAccount = linkedId != null;

        if (hadLinkedAccount) {
            Member member = DiscordSRV.getPlugin().getMainGuild().getMemberById(linkedId);
            String name = member != null ? member.getEffectiveName() : "Discord ID " + linkedId;

            sender.sendMessage(ChatColor.AQUA + LangUtil.InternalMessage.UNLINK_SUCCESS.toString()
                    .replace("{name}", name)
            );
        } else {
            sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.UNLINK_FAIL.toString());
        }
    }

}
