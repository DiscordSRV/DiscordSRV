package github.scarsz.discordsrv.objects.metrics;

import com.google.gson.*;
import github.scarsz.discordsrv.DiscordSRV;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/13/2017
 * @at 12:58 PM
 */
public class MetricsManager {

    private Map<String, AtomicInteger> metrics = new HashMap<>();
    private final File metricsFile;

    public MetricsManager(File metricsFile) {
        this.metricsFile = metricsFile;
        if (!metricsFile.exists()) return;

        try {
            String json = "";
            for (String s : FileUtils.readFileToString(metricsFile, Charset.defaultCharset()).split("\\[|, |]"))
                if (!s.trim().isEmpty()) json += Character.toChars(Integer.parseInt(s))[0];

            for (Map.Entry<String, JsonElement> entry : new Gson().fromJson(json, JsonObject.class).entrySet())
                metrics.put(entry.getKey(), new AtomicInteger(entry.getValue().getAsInt()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        if (metrics.size() == 0) {
            DiscordSRV.info("Skipped saving metrics because there were none");
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            JsonObject map = new JsonObject();
            metrics.forEach((key, atomicInteger) -> map.addProperty(key, atomicInteger.intValue()));
            FileUtils.writeStringToFile(metricsFile, Arrays.toString(map.toString().getBytes()), Charset.defaultCharset());
        } catch (IOException e) {
            DiscordSRV.error("Failed saving metrics: " + e.getMessage());
            return;
        }

        DiscordSRV.info("Saved metrics in " + (System.currentTimeMillis() - startTime) + "ms");
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
