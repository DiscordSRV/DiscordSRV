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
                DiscordSRV.error("Broke from Channel Topic Updater thread: sleep interrupted");
                return;
            }
        }
    }

    @SuppressWarnings({"SpellCheckingInspection", "ConstantConditions"})
    private static String applyPlaceholders(String input) {
        if (StringUtils.isBlank(input)) return "";

        final Map<String, String> mem = MemUtil.get();

        input = input.replaceAll("%time%|%date%", TimeUtil.timeStamp())
                     .replace("%playercount%", Integer.toString(PlayerUtil.getOnlinePlayers(true).size()))
                     .replace("%playermax%", Integer.toString(Bukkit.getMaxPlayers()))
                     .replace("%totalplayers%", Integer.toString(DiscordSRV.getTotalPlayerCount()))
                     .replace("%uptimemins%", Long.toString(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime())))
                     .replace("%uptimehours%", Long.toString(TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime())))
                     .replace("%uptimedays%", Long.toString(TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - DiscordSRV.getPlugin().getStartTime())))
                     .replace("%motd%", StringUtils.isNotBlank(Bukkit.getMotd()) ? DiscordUtil.strip(Bukkit.getMotd()) : "")
                     .replace("%serverversion%", Bukkit.getBukkitVersion())
                     .replace("%freememory%", mem.get("freeMB"))
                     .replace("%usedmemory%", mem.get("usedMB"))
                     .replace("%totalmemory%", mem.get("totalMB"))
                     .replace("%maxmemory%", mem.get("maxMB"))
                     .replace("%freememorygb%", mem.get("freeGB"))
                     .replace("%usedmemorygb%", mem.get("usedGB"))
                     .replace("%totalmemorygb%", mem.get("totalGB"))
                     .replace("%maxmemorygb%", mem.get("maxGB"))
                     .replace("%tps%", Lag.getTPSString());

        return input;
    }

}
