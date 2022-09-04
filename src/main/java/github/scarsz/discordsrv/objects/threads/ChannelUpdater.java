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

import alexh.weak.Dynamic;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import lombok.Getter;
import net.dv8tion.jda.api.entities.GuildChannel;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

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

        final List<Dynamic> configEntries = DiscordSRV.config().get("ChannelUpdater");

        for (final Dynamic configEntry : configEntries) {
            final String channelId = configEntry.get("ChannelId").asString();
            final String format = configEntry.get("Format").asString();
            final String intervalAsString = configEntry.get("UpdateInterval").asString();
            final String shutdownFormat = configEntry.get("ShutdownFormat").asString();
            final int interval;

            if (channelId.equals("0000000000000000")) continue; // Ignore default

            if (StringUtils.isAnyBlank(channelId, format)) {
                DiscordSRV.debug(Debug.CHANNEL_UPDATER, "Failed to initialise a ChannelUpdater entry: Missing either ChannelId or Format");
                continue;
            }
            if (StringUtils.isNotBlank(intervalAsString) && StringUtils.isNumeric(intervalAsString)) {
                interval = Integer.parseInt(intervalAsString);
            } else {
                DiscordSRV.warning("Update interval in minutes provided for Updater Channel " + channelId + " was blank or invalid, using the minimum value of 10");
                interval = 10;
            }

            final GuildChannel channel = DiscordUtil.getJda().getGuildChannelById(channelId);
            if (channel == null) {
                DiscordSRV.error("ChannelUpdater entry " + channelId + " has an invalid id");
                continue;
            }
            DiscordSRV.debug(Debug.CHANNEL_UPDATER, "Initialising ChannelUpdater entry " + channelId);
            UpdaterChannel updaterChannel = new UpdaterChannel(channel, format, interval, shutdownFormat);
            this.updaterChannels.add(updaterChannel);
            updaterChannel.update();
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
                wait(TimeUnit.MINUTES.toMillis(1));

                for (final UpdaterChannel channel : this.updaterChannels) {
                    channel.performTick();
                }
            } catch (InterruptedException e) {
                DiscordSRV.debug(Debug.CHANNEL_UPDATER, "Broke from Channel Updater thread: sleep interrupted");
                return;
            }
        }
    }

    public static class UpdaterChannel {

        public static final int MAX_CHANNEL_NAME = 100;
        @Getter private final String channelId;
        @Getter private final String format;
        @Getter private final int interval;
        @Getter @Nullable private final String shutdownFormat;
        private int minutesUntilRefresh;

        public UpdaterChannel(GuildChannel channel, String format, int interval, @Nullable String shutdownFormat) {
            this.channelId = channel.getId();
            this.format = format;
            this.shutdownFormat = shutdownFormat;

            // Minimum value for the interval is 10 so we'll make sure it's above that
            if (interval < 10) {
                DiscordSRV.warning("Update interval in minutes for channel \"" + channel.getName() + "\" was below the minimum value of 10. Using 10 as the interval.");
                this.interval = 10;
            } else this.interval = interval;

            this.minutesUntilRefresh = this.interval;

        }

        public void update() {
            final GuildChannel discordChannel = DiscordUtil.getJda().getGuildChannelById(this.channelId);
            if (discordChannel == null) {
                DiscordSRV.error(String.format("Failed to find channel \"%s\". Does it exist?", this.channelId));
                return;
            }

            String newName = PlaceholderUtil.replaceChannelUpdaterPlaceholders(this.format);
            parseChannelName(discordChannel, newName, false);
        }

        public void updateToShutdownFormat() {
            if (this.shutdownFormat == null) return;

            final GuildChannel discordChannel = StringUtils.isNotBlank(this.channelId) && StringUtils.isNumeric(this.channelId)
                    ? DiscordUtil.getTextChannelById(this.channelId)
                    : null;
            if (discordChannel == null) {
                DiscordSRV.error(String.format("Failed to find channel \"%s\". Does it exist?", this.channelId));
                return;
            }

            String newName = this.shutdownFormat
                    .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                    .replace("%serverversion%", Bukkit.getBukkitVersion())
                    .replace("%totalplayers%", Integer.toString(DiscordSRV.getTotalPlayerCount()))
                    .replace("%timestamp%", Long.toString(System.currentTimeMillis() / 1000));

            parseChannelName(discordChannel, newName, true);
        }

        public void performTick() {
            this.minutesUntilRefresh--;

            if (this.minutesUntilRefresh <= 0) {
                this.update();
                this.minutesUntilRefresh = this.interval;
            }
        }

        private void parseChannelName(GuildChannel discordChannel, String newName, boolean blockThread) {
            if (newName.length() > MAX_CHANNEL_NAME) {
                newName = newName.substring(0, MAX_CHANNEL_NAME - 1);
                DiscordSRV.debug(Debug.CHANNEL_UPDATER, "The new channel name for \"" + discordChannel.getName() + "\" was too long. Reducing it to " + MAX_CHANNEL_NAME + " characters...");
                if (StringUtils.isBlank(newName)) {
                    DiscordSRV.debug(Debug.CHANNEL_UPDATER, "The new channel name for `\"" + discordChannel.getName() + "\" was blank, skipping...");
                    return;
                }
            }

            DiscordUtil.setChannelName(discordChannel, newName, blockThread);
        }
    }
}
