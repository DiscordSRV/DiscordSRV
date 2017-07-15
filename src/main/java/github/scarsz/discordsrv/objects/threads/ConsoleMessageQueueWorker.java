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

    @Override
    public void run() {
        while (true) {
            try {
                StringBuilder message = new StringBuilder();
                String line = DiscordSRV.getPlugin().getConsoleMessageQueue().poll();
                while (line != null) {
                    if (message.length() + line.length() + 1 > 2000) {
                        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getConsoleChannel(), message.toString());
                        message = new StringBuilder();
                    }
                    message.append(line).append("\n");

                    line = DiscordSRV.getPlugin().getConsoleMessageQueue().poll();
                }

                if (StringUtils.isNotBlank(message.toString().replace("\n", "")))
                    DiscordUtil.sendMessage(DiscordSRV.getPlugin().getConsoleChannel(), message.toString());

                // make sure rate isn't less than every second because of rate limitations
                // even then, a console channel update /every second/ is pushing it
                int sleepTime = DiscordSRV.getPlugin().getConfig().getInt("DiscordConsoleChannelLogRefreshRateInSeconds") * 1000;
                if (sleepTime < 1000) sleepTime = 1000;

                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Console Message Queue Worker thread: interrupted");
                return;
            }
        }
    }

}
