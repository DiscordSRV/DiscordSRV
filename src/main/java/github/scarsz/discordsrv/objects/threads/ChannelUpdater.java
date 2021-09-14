/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import lombok.Getter;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChannelUpdater extends Thread {

    @Getter private final Set<UpdaterChannel> updaterChannels = new HashSet<>();

    public ChannelUpdater() {
        setName("DiscordSRV - Channel Updater");
    }

    public void reload() {
        // Deleting and recreating the list of updater channels
        this.updaterChannels.clear();

        final List<Map<String, Object>> configEntries = DiscordSRV.config().getList("ChannelUpdater");
        for (final Map<String, Object> configEntry : configEntries) {
            final String channelId = configEntry.get("ChannelId").toString();
            final String format = configEntry.get("Format").toString();
            final String intervalAsString = configEntry.get("UpdateInterval").toString();
            final int interval;

            if (channelId.equals("0000000000000000")) continue; // Ignore default

            if (StringUtils.isAnyBlank(channelId, format)) {
                DiscordSRV.error("Failed to initialise a ChannelUpdater entry: Missing either ChannelId or Format");
                continue;
            }
            if (StringUtils.isNotBlank(intervalAsString) && StringUtils.isNumeric(intervalAsString)) {
                interval = Integer.parseInt(intervalAsString);
            } else {
                DiscordSRV.debug(Debug.CHANNEL_UPDATER, "Update interval provided for Updater Channel " + channelId + " was blank or invalid, using the default value of 10");
                interval = 10;
            }

            final GuildChannel channel = DiscordUtil.getJda().getGuildChannelById(channelId);
            if (channel == null) {
                DiscordSRV.error("ChannelUpdater entry " + channelId + " has an invalid id");
                continue;
            }
            DiscordSRV.debug(Debug.CHANNEL_UPDATER, "Initialising ChannelUpdater entry " + channelId);
            this.updaterChannels.add(new UpdaterChannel(channel, format, interval));
        }
    }

    @Override
    public void run() {
        reload();
        for (final UpdaterChannel channel : this.updaterChannels) {
            channel.update();
        }
        while (true) {
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));

                for (final UpdaterChannel channel : this.updaterChannels) {
                    channel.performTick();
                }
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Channel Updater thread: sleep interrupted");
                return;
            }
        }
    }

    private static class UpdaterChannel {

        @Getter private final GuildChannel discordChannel;
        @Getter private final String format;
        @Getter private final int interval;
        private int minutesUntilRefresh;

        public UpdaterChannel(GuildChannel channel, String format, int interval) {
            this.discordChannel = channel;
            this.format = format;

            // Minimum value for the interval is 10 so we'll make sure it's above that
            if (interval < 10) {
                DiscordSRV.debug(Debug.CHANNEL_UPDATER, "Update interval for channel \"" + channel.getName() + "\" was below the minimum value of 10. Using 10 as the interval.");
                this.interval = 10;
            } else this.interval = interval;

            this.minutesUntilRefresh = this.interval;

        }

        public void update () {
            String message = PlaceholderUtil.replaceChannelUpdaterPlaceholders(this.format);
            if (message.length() > 100) {
                message = message.substring(0, 99);
                DiscordSRV.debug(Debug.CHANNEL_UPDATER, "The new channel name for \"" + this.discordChannel.getName() + "\" was too long. Reducing it to 100 characters...");
                if (StringUtils.isBlank(message)) {
                    DiscordSRV.debug(Debug.CHANNEL_UPDATER, "The new channel name for `\"" + this.discordChannel.getName() + "\" was blank, skipping...");
                    return;
                }
            }
            try {
                this.discordChannel.getManager().setName(message).queue();
            } catch (Exception e) {
                if (e instanceof PermissionException) {
                    final PermissionException pe = (PermissionException) e;
                    if (pe.getPermission() != Permission.UNKNOWN) {
                        DiscordSRV.warning(String.format("Could not rename channel \"%s\" because the bot does not have the \"%s\" permission.", this.discordChannel.getName(), pe.getPermission().getName()));
                    }
                } else {
                    DiscordSRV.warning(String.format("Could not rename channel \"%s\" because \"%s\"", this.discordChannel.getName(), e.getMessage()));
                }
            }
        }

        public void performTick () {
            this.minutesUntilRefresh --;

            if (this.minutesUntilRefresh <= 0) {
                this.update();
                this.minutesUntilRefresh = this.interval;
            }
        }
    }
}
