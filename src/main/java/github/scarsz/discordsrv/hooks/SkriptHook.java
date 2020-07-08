package github.scarsz.discordsrv.hooks;

import ch.njol.skript.ScriptLoader;
import github.scarsz.discordsrv.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SkriptHook {

    public static Set<String> getSkripts() {
        Set<String> scripts = new HashSet<>();

        if (!PluginUtil.checkIfPluginEnabled("skript")) {
            scripts.add("skript not found/enabled");
            return scripts;
        }

        try {
            for (File file : ScriptLoader.getLoadedFiles()) {
                boolean cancelsChatEvents = false;

                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    boolean treeIsOnChat = false;

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (StringUtils.isBlank(line)) continue;

                        if (!StringUtils.isWhitespace(line.substring(0, 1))) {
                            treeIsOnChat = StringUtils.startsWithIgnoreCase(line, "on chat");
                        }

                        if (treeIsOnChat && StringUtils.startsWithIgnoreCase(line, "cancel")) {
                            cancelsChatEvents = true;
                            break;
                        }
                    }
                }

                scripts.add(file.getName() + (cancelsChatEvents ? " [CANCELS CHAT]" : ""));
            }
        } catch (Exception e) {
            scripts = new HashSet<>(Collections.singletonList("exception: " + e.getMessage()));
        }

        if (scripts.isEmpty()) {
            scripts.add("none");
        }

        return scripts;
    }

}
