package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import org.bukkit.entity.Player;

public class CommandUnsubscribe {

    @Command(commandNames = { "unsubscribe" },
            helpMessage = "Unsubscribes yourself from Discord messages",
            permission = "discordsrv.unsubscribe"
    )
    public static void execute(Player sender, String[] args) {
        DiscordSRV.getPlugin().setIsSubscribed(sender.getUniqueId(), false);

        sender.sendMessage(LangUtil.Message.ON_UNSUBSCRIBE.toString());
    }

}
