package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 8:37 PM
 */
public class ServerWatchdog extends Thread {

    public ServerWatchdog() {
        super("DiscordSRV - Server Watchdog");
    }

    private long lastTick = System.currentTimeMillis();

    private void tick() {
        lastTick = System.currentTimeMillis();
    }

    @Override
    public void run() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(DiscordSRV.getPlugin(), this::tick, 0, 20);

        while (true) {
            try {
                int timeout = DiscordSRV.getPlugin().getConfig().getInt("ServerWatchdogTimeout");
                if (timeout < 10) timeout = 10; // minimum value
                if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastTick) < timeout) {
                    Thread.sleep(1000);
                } else {
                    if (!DiscordSRV.getPlugin().getConfig().getBoolean("ServerWatchdogEnabled")) {
                        DiscordSRV.debug("The Server Watchdog would have triggered right now but it was disabled in the config");
                        return;
                    }

                    for (int i = 0; i < DiscordSRV.getPlugin().getConfig().getInt("ServerWatchdogMessageCount"); i++) {
                        DiscordSRV.getPlugin().getMainTextChannel().sendMessage(LangUtil.Message.SERVER_WATCHDOG.toString()
                                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                                .replace("%guildowner%", DiscordSRV.getPlugin().getMainGuild().getOwner().getAsMention())
                        );
                    }

                    return;
                }
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Server Watchdog thread: interrupted");
                return;
            }
        }
    }

}
