/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
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
import github.scarsz.discordsrv.api.events.WatchdogMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.WatchdogMessagePreProcessEvent;
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

                    String channelName = DiscordSRV.getPlugin().getMainTextChannel().getName();
                    String message = PlaceholderUtil.replacePlaceholders(LangUtil.Message.SERVER_WATCHDOG.toString());
                    int count = DiscordSRV.config().getInt("ServerWatchdogMessageCount");

                    WatchdogMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new WatchdogMessagePreProcessEvent(channelName, message, count, false));
                    if (preEvent.isCancelled()) {
                        DiscordSRV.debug("WatchdogMessagePreProcessEvent was cancelled, message send aborted");
                        return;
                    }
                    // Update from event in case any listeners modified parameters
                    count = preEvent.getCount();
                    channelName = preEvent.getChannel();
                    message = preEvent.getMessage();

                    String discordMessage = message
                            .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                            .replace("%guildowner%", DiscordSRV.getPlugin().getMainGuild().getOwner().getAsMention());

                    WatchdogMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new WatchdogMessagePostProcessEvent(channelName, discordMessage, count, false));
                    if (postEvent.isCancelled()) {
                        DiscordSRV.debug("WatchdogMessagePostProcessEvent was cancelled, message send aborted");
                        return;
                    }
                    // Update from event in case any listeners modified parameters
                    count = postEvent.getCount();
                    channelName = postEvent.getChannel();
                    discordMessage = postEvent.getProcessedMessage();

                    TextChannel channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);

                    for (int i = 0; i < count; i++) {
                        DiscordUtil.sendMessage(channel, discordMessage);
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
