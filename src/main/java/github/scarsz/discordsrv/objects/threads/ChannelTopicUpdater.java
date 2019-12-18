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
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.util.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChannelTopicUpdater extends Thread {

    public ChannelTopicUpdater() {
        setName("DiscordSRV - Channel Topic Updater");
    }

    @Override
    public void run() {
        while (true) {
            int rate = DiscordSRV.config().getInt("ChannelTopicUpdaterRateInMinutes");
            if (rate < 5) rate = 5;

            if (DiscordUtil.getJda() != null) {
                String chatTopic = applyLegacyPlaceholders(LangUtil.Message.CHAT_CHANNEL_TOPIC.toString()).replaceAll("%time%|%date%", notNull(TimeUtil.timeStamp()));
                chatTopic = PlaceholderUtil.applyAll(null, null, chatTopic);
                if (StringUtils.isNotBlank(chatTopic))
                    DiscordUtil.setTextChannelTopic(DiscordSRV.getPlugin().getMainTextChannel(), chatTopic);

                String consoleTopic = PlaceholderUtil.applyAll(null, null, LangUtil.Message.CONSOLE_CHANNEL_TOPIC.toString());
                if (StringUtils.isNotBlank(consoleTopic))
                    DiscordUtil.setTextChannelTopic(DiscordSRV.getPlugin().getConsoleChannel(), consoleTopic);
            } else {
                DiscordSRV.debug("Skipping channel topic update cycle, JDA was null");
            }

            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(rate));
            } catch (InterruptedException e) {
                DiscordSRV.warning("Broke from Channel Topic Updater thread: sleep interrupted");
                return;
            }
        }
    }

    private String applyLegacyPlaceholders(String text) {
        String[] legacyPlaceholders = {"playercount","playermax","totalplayers","uptimemins","uptimehours","uptimedays",
                "motd","serverversion","freememory","usedmemory","totalmemory","maxmemory","freememorygb","usedmemorygb","totalmemorygb","maxmemorygb","tps"};
        for (String legacyPlaceholder : legacyPlaceholders) {
            if (text.contains(legacyPlaceholder))
                DiscordSRV.info("Found legacy placeholder " + legacyPlaceholder + " in " + text + ". This should be replaced with %server_" + legacyPlaceholder + "%");
            text = text.replace("%" + legacyPlaceholder + "%", "%server_" + legacyPlaceholder + "%");
        }
        return text;
    }
    private static String notNull(Object object) {
        return object != null ? object.toString() : "";
    }
}
