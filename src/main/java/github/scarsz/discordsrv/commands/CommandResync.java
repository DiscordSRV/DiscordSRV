package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.concurrent.TimeUnit;

public class CommandResync {

    @Command(commandNames = { "resync" },
            helpMessage = "Resynchronizes all groups & roles",
            permission = "discordsrv.resync"
    )
    public static void execute(CommandSender sender, String[] args) {
        if (!DiscordSRV.config().getBoolean("GroupRoleSynchronizationEnabled")) {
            sender.sendMessage(ChatColor.RED + "Group synchonization is disabled. Please set GroupRoleSynchronizationEnabled to true in synchronization.yml to use this feature.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            sender.sendMessage(ChatColor.AQUA + "Full group synchronization triggered.");
            long time = System.currentTimeMillis();
            DiscordSRV.getPlugin().getGroupSynchronizationManager().resyncEveryone();
            time = System.currentTimeMillis() - time;
            int seconds = Math.toIntExact(TimeUnit.MILLISECONDS.toSeconds(time));
            sender.sendMessage(ChatColor.AQUA + "Full group synchronization finished, taking " + seconds + " seconds.");
        });
    }

}
