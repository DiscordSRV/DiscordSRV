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

import alexh.weak.Dynamic;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.MemUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PlayingStatusUpdater extends Thread {
    public PlayingStatusUpdater() {
        setName("DiscordSRV - Status Updater");
    }

    private int lastStatus = 0;
    @Override
    public void run() {
        while (true) {
            int rate;
            // Breaking change to config
            try {
                rate = DiscordSRV.config().getInt("StatusUpdateRateInMinutes");
            } catch (IllegalArgumentException e) {
                DiscordSRV.warning("\"StatusUpdaterRateInMinutes\" not found in config");
                rate = 2;
            }

            if (rate < 1) rate = 1;

            if (DiscordUtil.getJda() != null) {

                Dynamic dynamic = DiscordSRV.config().dget("DiscordGameStatus");
                List<String> statuses = new LinkedList<>();
                if (dynamic.isList()) {
                    statuses.addAll(dynamic.asList());
                } else {
                    statuses.add(dynamic.convert().intoString());
                }
                String status = applyPlaceholders(statuses.get(lastStatus));

                // Increment and wrap around
                lastStatus++;
                if (lastStatus == statuses.size() - 1)
                    lastStatus = 0;

                if (!StringUtils.isEmpty(status))
                    DiscordUtil.setGameStatus(status);
                else
                    DiscordSRV.debug("Skipping status update cycle, status was null");
            } else {
                DiscordSRV.debug("Skipping status update cycle, JDA was null");
            }

            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(rate));
            } catch (InterruptedException e) {
                DiscordSRV.error("Broke from Status Updater thread: sleep interrupted");
                return;
            }
        }
    }

    @Getter private static File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder().getAbsolutePath(), "/playerdata");
    @SuppressWarnings({"SpellCheckingInspection", "ConstantConditions"})
    private static String applyPlaceholders(String input) {
        if (StringUtils.isBlank(input)) return "";

        final Map<String, String> mem = MemUtil.get();

        input = input.replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%playercount%", Integer.toString(PlayerUtil.getOnlinePlayers(true).size()))
                .replace("%playermax%", Integer.toString(Bukkit.getMaxPlayers()))
                .replace("%totalplayers%", Integer.toString(playerDataFolder.listFiles(f -> f.getName().endsWith(".dat")).length))
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
