/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.PermissionException;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

public class ChannelTopicUpdater extends Thread {

    boolean lastUpdateChatChannelTopicFailed = false;
    boolean lastUpdateConsoleChannelTopicFailed = false;

    public ChannelTopicUpdater() {
        setName("DiscordSRV - Channel Topic Updater");
    }

    boolean updateTopic(TextChannel channel, String topic, boolean lastFailed) {
        if (StringUtils.isNotBlank(topic)) {
            try {
                DiscordUtil.setTextChannelTopic(channel, topic);
            } catch (PermissionException e) {
                if (!lastFailed) {
                    DiscordSRV.warning("The bot requires the permission " + e.getPermission()
                            + " to set topic for the channel #" + channel.getName()
                            + ". Topic updating will not work until permission is given.");

                    return true;
                }
            }

            return false;
        }

        return lastFailed;
    }

    void updateChatChannelTopic() {
        TextChannel channel = DiscordSRV.getPlugin().getMainTextChannel();
        String topic = PlaceholderUtil.replaceChannelUpdaterPlaceholders(LangUtil.Message.CHAT_CHANNEL_TOPIC.toString());

        lastUpdateChatChannelTopicFailed = updateTopic(channel, topic, lastUpdateChatChannelTopicFailed);
    }

    void updateConsoleChannelTopic() {
        TextChannel channel = DiscordSRV.getPlugin().getConsoleChannel();
        String topic = PlaceholderUtil.replaceChannelUpdaterPlaceholders(LangUtil.Message.CONSOLE_CHANNEL_TOPIC.toString());

        lastUpdateConsoleChannelTopicFailed = updateTopic(channel, topic, lastUpdateConsoleChannelTopicFailed);
    }

    @Override
    public void run() {
        while (true) {
            int rate = DiscordSRV.config().getInt("ChannelTopicUpdaterRateInMinutes");
            if (rate < 10) rate = 10;

            if (DiscordUtil.getJda() != null) {
                updateChatChannelTopic();
                updateConsoleChannelTopic();
            } else {
                DiscordSRV.debug("Skipping channel topic update cycle, JDA was null");
            }

            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(rate));
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Channel Topic Updater thread: sleep interrupted");
                return;
            }
        }
    }
}
