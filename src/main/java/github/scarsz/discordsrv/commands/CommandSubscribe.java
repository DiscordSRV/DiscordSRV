package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import org.bukkit.entity.Player;

public class CommandSubscribe {

    @Command(commandNames = { "subscribe" },
            helpMessage = "Subscribes yourself to Discord messages",
            permission = "discordsrv.subscribe"
    )
    public static void execute(Player sender, String[] args) {
        DiscordSRV.getPlugin().setIsSubscribed(sender.getUniqueId(), true);

        sender.sendMessage(LangUtil.Message.ON_SUBSCRIBE.toString());
    }

}
