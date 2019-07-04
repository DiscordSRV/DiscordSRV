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

package github.scarsz.discordsrv.objects.managers;

import com.google.gson.*;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MetricsManager {

    private Map<String, AtomicInteger> metrics = new HashMap<>();
    private final File metricsFile;

    public MetricsManager(File metricsFile) {
        this.metricsFile = metricsFile;
        if (!metricsFile.exists() || metricsFile.length() == 0) return;

        try {
            StringBuilder json = new StringBuilder();
            for (String s : FileUtils.readFileToString(metricsFile, Charset.forName("UTF-8")).split("\\[|, |]"))
                if (!s.trim().isEmpty()) json.append(Character.toChars(Integer.parseInt(s))[0]);

            for (Map.Entry<String, JsonElement> entry : new Gson().fromJson(json.toString(), JsonObject.class).entrySet())
                metrics.put(entry.getKey(), new AtomicInteger(entry.getValue().getAsInt()));
        } catch (IOException e) {
            System.out.println("Failed loading Metrics: " + e.getMessage());
            metricsFile.delete();
        }
    }

    public void save() {
        if (metrics.size() == 0) {
            DiscordSRV.info(LangUtil.InternalMessage.METRICS_SAVE_SKIPPED);
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            JsonObject map = new JsonObject();
            metrics.forEach((key, atomicInteger) -> map.addProperty(key, atomicInteger.intValue()));
            FileUtils.writeStringToFile(metricsFile, Arrays.toString(map.toString().getBytes()), Charset.forName("UTF-8"));
        } catch (IOException e) {
            DiscordSRV.error(LangUtil.InternalMessage.METRICS_SAVE_FAILED + ": " + e.getMessage());
            return;
        }

        DiscordSRV.info(LangUtil.InternalMessage.METRICS_SAVED.toString()
                .replace("{ms}", String.valueOf(System.currentTimeMillis() - startTime))
        );
    }

    public void increment(String key) {
        if (metrics.containsKey(key.toLowerCase())) {
            metrics.get(key.toLowerCase()).getAndIncrement();
        } else {
            metrics.put(key.toLowerCase(), new AtomicInteger(1));
        }
    }
    public int get(String key) {
        return metrics.containsKey(key.toLowerCase()) ? metrics.get(key.toLowerCase()).intValue() : 0;
    }

}
