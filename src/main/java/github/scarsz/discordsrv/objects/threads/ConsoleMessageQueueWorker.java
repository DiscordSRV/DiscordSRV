package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.apache.commons.lang3.StringUtils;

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
            String line = DiscordSRV.getPlugin().getConsoleMessageQueue().poll();
            while (line != null) {
                if (message.length() + line.length() + 1 > 2000) {
                    DiscordUtil.sendMessage(DiscordSRV.getPlugin().getConsoleChannel(), message);
                    message = "";
                }
                message += line + "\n";

                line = DiscordSRV.getPlugin().getConsoleMessageQueue().poll();
            }

            if (StringUtils.isNotBlank(message.replace("\n", "")))
                DiscordUtil.sendMessage(DiscordSRV.getPlugin().getConsoleChannel(), message);

            try {
                int sleepTime = DiscordSRV.getPlugin().getConfig().getInt("DiscordConsoleChannelLogRefreshRate");
                if (sleepTime < 1000) sleepTime = 1000;

                Thread.sleep(sleepTime);
            } catch (Exception ignored) {}
        }
    }

}
