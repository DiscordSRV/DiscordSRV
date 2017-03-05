package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ManifestUtil {

    private static Map<String, String> attributes = new HashMap<>();

    static {
        try {
            Enumeration<URL> resources = DiscordSRV.getPlugin().getClass().getClassLoader().getResources(JarFile.MANIFEST_NAME);

            while (resources.hasMoreElements()) {
                InputStream inputStream = resources.nextElement().openStream();
                Manifest manifest = new Manifest(inputStream);
                for (Map.Entry<Object, Object> manifestEntry : manifest.getMainAttributes().entrySet())
                    attributes.put((String) manifestEntry.getKey(), (String) manifestEntry.getValue());
                inputStream.close();
            }
        } catch (IOException ignored) {}
    }

    static String getManifestValue(String key) {
        return attributes.get(key);
    }

}
