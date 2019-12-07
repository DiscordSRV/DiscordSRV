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
import github.scarsz.discordsrv.util.DiscordUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PresenceUpdater extends Thread {
    public PresenceUpdater() {
        setName("DiscordSRV - Presence Updater");
    }

    private int lastStatus = 0;
    @Override
    public void run() {
        while (true) {
            int rate;
            try {
                rate = DiscordSRV.config().getInt("StatusUpdateRateInMinutes");
            } catch (IllegalArgumentException e) {
                DiscordSRV.warning("\"StatusUpdateRateInMinutes\" not found in config");
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
                DiscordSRV.debug("Loaded statuses: "+statuses);
                String status = statuses.get(lastStatus);
                DiscordSRV.debug("Setting presence to \""+status+"\", id "+lastStatus);

                // Increment and wrap around
                lastStatus++;
                if (lastStatus == statuses.size()) lastStatus = 0;

                if (!StringUtils.isEmpty(status)) {
                    DiscordUtil.setGameStatus(status);
                    DiscordSRV.debug("Presence set to \""+status+"\"");
                } else DiscordSRV.debug("Skipping status update cycle, status was null");
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
}
