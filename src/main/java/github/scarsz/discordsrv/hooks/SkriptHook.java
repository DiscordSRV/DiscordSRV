/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

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
                            treeIsOnChat = StringUtils.startsWithIgnoreCase(line.trim(), "on chat");
                        }

                        if (treeIsOnChat && StringUtils.startsWithIgnoreCase(line.trim(), "cancel")) {
                            cancelsChatEvents = true;
                            break;
                        }
                    }
                }

                scripts.add(file.getName() + (cancelsChatEvents ? " [CANCELS CHAT]" : ""));
            }
        } catch (Throwable t) {
            scripts = new HashSet<>(Collections.singletonList("exception: " + t.getMessage()));
        }

        if (scripts.isEmpty()) {
            scripts.add("none");
        }

        return scripts;
    }

}
