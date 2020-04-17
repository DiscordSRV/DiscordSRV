package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
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
        if (!DiscordSRV.getPlugin().isGroupRoleSynchronizationEnabled()) {
            sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.RESYNC_WHEN_GROUP_SYNC_DISABLED.toString());
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
