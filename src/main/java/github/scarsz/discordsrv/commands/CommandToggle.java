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
public class CommandToggle {

    @Command(commandNames = { "toggle" },
            helpMessage = "Toggles receiving Discord messages for yourself",
            permission = "discordsrv.toggle")
    public static void execute(Player sender, String[] args) {
        DiscordSRV.getPlugin().setIsSubscribed(sender.getUniqueId(), DiscordSRV.getPlugin().getUnsubscribedPlayers().contains(sender.getUniqueId()));

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', !DiscordSRV.getPlugin().getUnsubscribedPlayers().contains(sender.getUniqueId())
                ? DiscordSRV.getPlugin().getConfig().getString("MinecraftSubscriptionMessagesOnSubscribe")
                : DiscordSRV.getPlugin().getConfig().getString("MinecraftSubscriptionMessagesOnUnsubscribe")
        ));
    }

}
