package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.core.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 3:31 PM
 */
@SuppressWarnings("deprecation")
public class CommandLinked {

    @Command(commandNames = { "linked" },
            helpMessage = "Checks what Discord user your (or someone else's) MC account is linked to",
            permission = "discordsrv.linked"
    )
    public static void execute(Player sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "Your UUID is linked to " + (DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId()) != null
                    ? DiscordSRV.getPlugin().getJda().getUserById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId())) != null
                        ? DiscordSRV.getPlugin().getJda().getUserById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId())) :
                        DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId())
                    : "nobody."));
        } else {
            String target = args[0];

            if (DiscordSRV.getPlugin().getJda().getUserById(target) != null) { // discord id given
                User targetUser = DiscordSRV.getPlugin().getJda().getUserById(target);
                UUID targetUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(target);
                OfflinePlayer targetPlayer = Bukkit.getPlayer(targetUuid);

                if (targetUuid == null)
                    sender.sendMessage(ChatColor.RED + "Discord user " + targetUser + " is not linked to any Minecraft account");
                else
                    sender.sendMessage(ChatColor.AQUA + "Discord user " + targetUser + " is linked to Minecraft account " + targetPlayer.getName() + " (UUID " + targetUuid + ")");
            } else if (target.length() == 32 || target.length() == 36) { // uuid given
                UUID targetUuid = UUID.fromString(target);
                OfflinePlayer targetPlayer = Bukkit.getPlayer(targetUuid);
                User targetUser = DiscordSRV.getPlugin().getJda().getUserById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(targetUuid));

                if (targetUser == null)
                    sender.sendMessage(ChatColor.RED + "Minecraft account " + targetPlayer.getName() + " (UUID " + targetUuid + ") is not linked to any Discord account");
                else
                    sender.sendMessage(ChatColor.AQUA + "Minecraft account " + targetPlayer.getName() + " (UUID " + targetUuid + ") is linked to Discord account " + targetUser);
            } else if (Bukkit.getPlayerExact(target) != null) { // player name given
                OfflinePlayer targetPlayer = Bukkit.getPlayerExact(target);
                UUID targetUuid = targetPlayer.getUniqueId();
                User targetUser = DiscordSRV.getPlugin().getJda().getUserById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(targetUuid));

                if (targetUser == null)
                    sender.sendMessage(ChatColor.RED + "Minecraft account " + targetPlayer.getName() + " (UUID " + targetUuid + ") is not linked to any Discord account");
                else
                    sender.sendMessage(ChatColor.AQUA + "Minecraft account " + targetPlayer.getName() + " (UUID " + targetUuid + ") is linked to Discord account " + targetUser);
            } else { // discord name given?
                List<User> matchingUsers = DiscordSRV.getPlugin().getJda().getUsersByName(String.join(" ", args), true);

                if (matchingUsers.size() == 0) {
                    sender.sendMessage(ChatColor.RED + "Nobody found with Discord ID/Discord name/Minecraft name/Minecraft UUID matching \"" + target + "\" to look up.");
                    return;
                }

                for (User targetUser : matchingUsers) {
                    UUID targetUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(targetUser.getId());
                    OfflinePlayer targetPlayer = Bukkit.getPlayer(targetUuid);

                    sender.sendMessage(ChatColor.AQUA + "Discord user " + targetUser + " is linked to Minecraft account " + targetPlayer.getName() + " (UUID " + targetUuid + ")");
                }
            }
        }
    }

}
