package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.core.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 10:27 PM
 */
public class PlayerBanListener implements Listener {

    public PlayerBanListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(DiscordSRV.getPlugin(), () -> {
            if (Bukkit.getBannedPlayers().contains(Bukkit.getOfflinePlayer(event.getPlayer().getUniqueId()))) {
                if (event.getPlayer() instanceof OfflinePlayer) {
                    if (!DiscordSRV.getPlugin().getConfig().getBoolean("BanSynchronizationMinecraftToDiscord")) {
                        DiscordSRV.debug("Not handling ban for player " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ") because doing so is disabled in the config");
                        return;
                    }

                    DiscordSRV.debug("Handling ban for player " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ")");
                    DiscordUtil.banMember(DiscordUtil.getMemberById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(event.getPlayer().getUniqueId())));
                }
            }
        }, 20);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!DiscordSRV.getPlugin().getConfig().getBoolean("BanSynchronizationMinecraftToDiscord")) {
            DiscordSRV.debug("Not handling possible unban for player " + event.getPlayer().getName() + " (" + event.getPlayer().getUniqueId() + ") because doing so is disabled in the config");
            return;
        }

        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(event.getPlayer().getUniqueId());
        if (discordId == null) return;
        User discordUser = DiscordUtil.getUserById(discordId);
        if (discordUser == null) return;

        boolean wasBanned = false;
        for (User user : DiscordSRV.getPlugin().getMainGuild().getController().getBans().complete())
            if (user.getId().equals(discordUser.getId()))
                wasBanned = true;
        if (!wasBanned) return;

        DiscordUtil.unbanUser(DiscordSRV.getPlugin().getMainGuild(), discordUser);
    }

}
