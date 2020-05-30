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
            if (rate < 10) rate = 10;

            if (DiscordUtil.getJda() != null) {
                String chatTopic = applyPlaceholders(LangUtil.Message.CHAT_CHANNEL_TOPIC.toString());
                if (StringUtils.isNotBlank(chatTopic))
                    DiscordUtil.setTextChannelTopic(DiscordSRV.getPlugin().getMainTextChannel(), chatTopic);

                String consoleTopic = applyPlaceholders(LangUtil.Message.CONSOLE_CHANNEL_TOPIC.toString());
                if (StringUtils.isNotBlank(consoleTopic))
                    DiscordUtil.setTextChannelTopic(DiscordSRV.getPlugin().getConsoleChannel(), consoleTopic);
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

    @SuppressWarnings({"SpellCheckingInspection"})
    private static String applyPlaceholders(String input) {
        if (StringUtils.isBlank(input)) return "";

        // set PAPI placeholders
        input = PlaceholderUtil.replacePlaceholdersToDiscord(input);

        final Map<String, String> mem = MemUtil.get();

        input = input.replaceAll("%time%|%date%", notNull(TimeUtil.timeStamp()))
                     .replace("%playercount%", notNull(Integer.toString(PlayerUtil.getOnlinePlayers(true).size())))
                     .replace("%playermax%", notNull(Integer.toString(Bukkit.getMaxPlayers())))
                     .replace("%totalplayers%", notNull(Integer.toString(DiscordSRV.getTotalPlayerCount())))
                     .replace("%uptimemins%", notNull(Long.toString(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                     .replace("%uptimehours%", notNull(Long.toString(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                     .replace("%uptimedays%", notNull(Long.toString(TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime()))))
                     .replace("%motd%", notNull(StringUtils.isNotBlank(Bukkit.getMotd()) ? DiscordUtil.strip(Bukkit.getMotd()) : ""))
                     .replace("%serverversion%", notNull(Bukkit.getBukkitVersion()))
                     .replace("%freememory%", notNull(mem.get("freeMB")))
                     .replace("%usedmemory%", notNull(mem.get("usedMB")))
                     .replace("%totalmemory%", notNull(mem.get("totalMB")))
                     .replace("%maxmemory%", notNull(mem.get("maxMB")))
                     .replace("%freememorygb%", notNull(mem.get("freeGB")))
                     .replace("%usedmemorygb%", notNull(mem.get("usedGB")))
                     .replace("%totalmemorygb%", notNull(mem.get("totalGB")))
                     .replace("%maxmemorygb%", notNull(mem.get("maxGB")))
                     .replace("%tps%", notNull(Lag.getTPSString()));

        return input;
    }

    private static String notNull(Object object) {
        return object != null ? object.toString() : "";
    }

}
