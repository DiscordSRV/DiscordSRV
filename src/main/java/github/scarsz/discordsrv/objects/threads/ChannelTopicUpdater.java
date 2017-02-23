package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.MemUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Date;
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

    public void run() {
        int rate = DiscordSRV.getPlugin().getConfig().getInt("ChannelTopicUpdaterRateInSeconds") * 1000;

        // make sure rate isn't less than every second because of rate limitations
        // even then, a channel topic update /every second/ is pushing it
        if (rate < 1000) rate = 1000;

        while (!isInterrupted())
        {
            try {
                String chatTopic = applyFormatters(DiscordSRV.getPlugin().getConfig().getString("ChannelTopicUpdaterChatChannelTopicFormat"));
                String consoleTopic = applyFormatters(DiscordSRV.getPlugin().getConfig().getString("ChannelTopicUpdaterConsoleChannelTopicFormat"));

                // interrupt if both text channels are unavailable
                if (DiscordSRV.getPlugin().getMainTextChannel() == null && DiscordSRV.getPlugin().getConsoleChannel() == null) interrupt();
                // interrupt if both text channel's desired topics are empty
                if (chatTopic.isEmpty() && consoleTopic.isEmpty()) interrupt();

                if (DiscordSRV.getPlugin().getJda() != null && DiscordSRV.getPlugin().getJda().getSelfUser() != null) {
                    if (!chatTopic.isEmpty()) DiscordUtil.setTextChannelTopic(DiscordSRV.getPlugin().getMainTextChannel(), chatTopic);
                    if (!consoleTopic.isEmpty()) DiscordUtil.setTextChannelTopic(DiscordSRV.getPlugin().getConsoleChannel(), consoleTopic);
                }
            } catch (NullPointerException ignored) {}

            try { Thread.sleep(rate); } catch (InterruptedException ignored) {}
        }
    }

    private static File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder().getAbsolutePath(), "/playerdata");
    @SuppressWarnings({"SpellCheckingInspection", "ConstantConditions"})
    public static String applyFormatters(String input) {
        final Map<String, String> mem = MemUtil.get();

        input = input
                .replace("%playercount%", Integer.toString(PlayerUtil.getOnlinePlayers().size()))
                .replace("%playermax%", Integer.toString(Bukkit.getMaxPlayers()))
                .replace("%date%", new Date().toString())
                .replace("%totalplayers%", Integer.toString(playerDataFolder.listFiles(f -> f.getName().endsWith(".dat")).length))
                .replace("%uptimemins%", Long.toString(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - DiscordSRV.getPlugin().startTime)))
                .replace("%uptimehours%", Long.toString(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - DiscordSRV.getPlugin().startTime)))
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
