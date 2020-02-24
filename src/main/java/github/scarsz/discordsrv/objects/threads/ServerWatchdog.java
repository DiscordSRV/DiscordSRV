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
import github.scarsz.discordsrv.api.events.WatchdogMessageEvent;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

public class ServerWatchdog extends Thread {

    public ServerWatchdog() {
        super("DiscordSRV - Server Watchdog");
    }

    private long lastTick = System.currentTimeMillis();
    private boolean hasBeenTriggered = true;

    private void tick() {
        lastTick = System.currentTimeMillis();
        hasBeenTriggered = false;
    }

    @Override
    public void run() {
        int taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(DiscordSRV.getPlugin(), this::tick, 0, 20);
        if (taskNumber == -1) {
            DiscordSRV.debug("Failed to schedule repeating task for server watchdog; returning");
            return;
        }

        while (true) {
            try {
                int timeout = DiscordSRV.config().getInt("ServerWatchdogTimeout");
                if (timeout < 10) timeout = 10; // minimum value
                if (hasBeenTriggered || TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastTick) < timeout) {
                    Thread.sleep(1000);
                } else {
                    if (!DiscordSRV.config().getBoolean("ServerWatchdogEnabled")) {
                        DiscordSRV.debug("The Server Watchdog would have triggered right now but it was disabled in the config");
                        return;
                    }

                    String message = PlaceholderUtil.replacePlaceholders(LangUtil.Message.SERVER_WATCHDOG.toString()
                            .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                            .replace("%guildowner%", DiscordSRV.getPlugin().getMainGuild().getOwner().getAsMention())
                    );

                    String channelName = DiscordSRV.getPlugin().getMainTextChannel().getName();
                    Integer count = DiscordSRV.config().getInt("ServerWatchdogMessageCount");

                    WatchdogMessageEvent event = DiscordSRV.api.callEvent(new WatchdogMessageEvent(channelName, message, count, false));
                    if (event.isCancelled()) {
                        DiscordSRV.debug("WatchdogMessageEvent was cancelled, message send aborted");
                        return;
                    }
                    count = event.getCount();         // update count from event in case any listeners modified it
                    channelName = event.getChannel(); // update channel from event in case any listeners modified it
                    message = event.getMessage();     // update message from event in case any listeners modified it

                    TextChannel channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);

                    for (int i = 0; i < count; i++) {
                        DiscordUtil.sendMessage(channel, message);
                    }

                    return;
                }
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Server Watchdog thread: sleep interrupted");
                return;
            }
        }
    }

}
