package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.MemUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/7/2017
 * @at 5:27 PM
 */
public class ChannelTopicUpdater extends Thread {

    public ChannelTopicUpdater() {
        setName("DiscordSRV - Channel Topic Updater");
    }

    @Override
    public void run() {
        while (true) {
            try {
                String chatTopic = applyPlaceholders(DiscordSRV.getPlugin().getConfig().getString("ChannelTopicUpdaterChatChannelTopicFormat"));
                String consoleTopic = applyPlaceholders(DiscordSRV.getPlugin().getConfig().getString("ChannelTopicUpdaterConsoleChannelTopicFormat"));

                // interrupt if both text channels are unavailable
                if (DiscordSRV.getPlugin().getMainTextChannel() == null && DiscordSRV.getPlugin().getConsoleChannel() == null) {
                    DiscordSRV.debug("Broke from Channel Topic Updater thread: chat channel and console channel were both null");
                    return;
                }
                // interrupt if both text channel's desired topics are empty
                if (chatTopic.isEmpty() && consoleTopic.isEmpty()) {
                    DiscordSRV.debug("Broke from Channel Topic Updater thread: chat channel topic and console channel topic messages were both empty");
                    return;
                }

                if (DiscordSRV.getPlugin().getJda() != null && DiscordSRV.getPlugin().getJda().getSelfUser() != null) {
                    if (!chatTopic.isEmpty()) DiscordUtil.setTextChannelTopic(DiscordSRV.getPlugin().getMainTextChannel(), chatTopic);
                    if (!consoleTopic.isEmpty()) DiscordUtil.setTextChannelTopic(DiscordSRV.getPlugin().getConsoleChannel(), consoleTopic);
                }
            } catch (NullPointerException ignored) {}

            try {
                // make sure rate isn't less than every second because of rate limitations
                // even then, a channel topic update /every second/ is pushing it
                int rate = DiscordSRV.getPlugin().getConfig().getInt("ChannelTopicUpdaterRateInSeconds") * 1000;
                if (rate < 1000) rate = 1000;

                Thread.sleep(rate);
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Channel Topic Updater thread: sleep interrupted");
                return;
            }
        }
    }

    @Getter private static File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder().getAbsolutePath(), "/playerdata");
    @SuppressWarnings({"SpellCheckingInspection", "ConstantConditions"})
    public static String applyPlaceholders(String input) {
        final Map<String, String> mem = MemUtil.get();

        input = input
                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%playercount%", Integer.toString(PlayerUtil.getOnlinePlayers().size()))
                .replace("%playermax%", Integer.toString(Bukkit.getMaxPlayers()))
                .replace("%totalplayers%", Integer.toString(playerDataFolder.listFiles(f -> f.getName().endsWith(".dat")).length))
                .replace("%uptimemins%", Long.toString(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime())))
                .replace("%uptimehours%", Long.toString(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime())))
                .replace("%motd%", DiscordUtil.stripColor(Bukkit.getMotd()))
                .replace("%serverversion%", Bukkit.getBukkitVersion())
                .replace("%freememory%", mem.get("freeMB"))
                .replace("%usedmemory%", mem.get("usedMB"))
                .replace("%totalmemory%", mem.get("totalMB"))
                .replace("%maxmemory%", mem.get("maxMB"))
                .replace("%freememorygb%", mem.get("freeGB"))
                .replace("%usedmemorygb%", mem.get("usedGB"))
                .replace("%totalmemorygb%", mem.get("totalGB"))
                .replace("%maxmemorygb%", mem.get("maxGB"))
                .replace("%tps%", Lag.getTPSString())
        ;

        return input;
    }

}
