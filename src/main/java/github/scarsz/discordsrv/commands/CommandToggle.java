package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import org.bukkit.entity.Player;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 3:06 PM
 */
public class CommandToggle {

    @Command(commandNames = { "toggle" },
            helpMessage = "Toggles receiving Discord messages for yourself",
            permission = "discordsrv.toggle"
    )
    public static void execute(Player sender, String[] args) {
        DiscordSRV.getPlugin().setIsSubscribed(sender.getUniqueId(), DiscordSRV.getPlugin().getUnsubscribedPlayers().contains(sender.getUniqueId()));

        sender.sendMessage(!DiscordSRV.getPlugin().getUnsubscribedPlayers().contains(sender.getUniqueId())
                ? LangUtil.Message.ON_SUBSCRIBE.toString()
                : LangUtil.Message.ON_UNSUBSCRIBE.toString()
        );
    }

}
