package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ManifestUtil {

    public static String getManifestInfo(String valueKey) {
        try {
            Enumeration resEnum = DiscordSRV.getPlugin().getClass().getClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (resEnum.hasMoreElements()) {
                URL url = (URL) resEnum.nextElement();
                InputStream is = url.openStream();
                if (is != null) {
                    Manifest manifest = new Manifest(is);
                    Attributes mainAttributes = manifest.getMainAttributes();
                    String value = mainAttributes.getValue(valueKey);
                    if (value != null) return value;
                }
            }
        } catch (IOException ignored) {}
        return null;
    }

}
