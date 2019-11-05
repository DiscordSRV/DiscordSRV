/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.lang3.StringUtils;

public class ConsoleMessageQueueWorker extends Thread {

    public ConsoleMessageQueueWorker() {
        super("DiscordSRV - Console Message Queue Worker");
    }

    @Override
    public void run() {
        while (true) {
            try {
                // don't process, if we get disconnected, another measure to prevent UnknownHostException spam
                if (DiscordUtil.getJda().getStatus() != JDA.Status.CONNECTED) {
                    Thread.sleep(3000);
                    continue;
                }

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
                int sleepTime = DiscordSRV.config().getInt("DiscordConsoleChannelLogRefreshRateInSeconds") * 1000;
                if (sleepTime < 1000) sleepTime = 1000;

                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Console Message Queue Worker thread: interrupted");
                return;
            }
        }
    }

}
