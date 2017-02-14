package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.apache.commons.lang.StringUtils;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 5:25 PM
 */
public class ConsoleMessageQueueWorker extends Thread {

    public ConsoleMessageQueueWorker() {
        super("DiscordSRV - Console Message Queue Worker");
    }

    public void run() {
        while (!isInterrupted()) {
            String message = "";

            synchronized (DiscordSRV.getPlugin().getConsoleMessageQueue()) {
                for (String line : DiscordSRV.getPlugin().getConsoleMessageQueue()) {
                    if (message.length() + line.length() + 1 > 2000) {
                        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getConsoleChannel(), message);
                        message = "";
                    }
                    message += line + "\n";
                }
                DiscordSRV.getPlugin().getConsoleMessageQueue().clear();
            }

            if (StringUtils.isNotBlank(message.replace("\n", "")))
                DiscordUtil.sendMessage(DiscordSRV.getPlugin().getConsoleChannel(), message);

            try { Thread.sleep(DiscordSRV.getPlugin().getConfig().getInt("DiscordConsoleChannelLogRefreshRate")); } catch (Exception ignored) {}
        }
    }

}
