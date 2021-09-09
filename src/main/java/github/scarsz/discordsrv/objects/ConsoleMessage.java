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

package github.scarsz.discordsrv.objects;

import github.scarsz.configuralize.DynamicConfig;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.*;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;

@Data
public class ConsoleMessage {

    private final String eventLevel;
    private final String line;

    private String formatted;

    @Override
    public String toString() {
        if (formatted != null) return formatted;

        String line = this.line;
        final DynamicConfig config = DiscordSRV.config();

        // return if this is not an okay level to send
        boolean isAnOkayLevel = false;
        for (String enabledLevel : config.getStringList("DiscordConsoleChannelLevels")) {
            if (eventLevel.equals(enabledLevel.toUpperCase())) {
                isAnOkayLevel = true;
                break;
            }
        }
        if (!isAnOkayLevel) return null;

        // remove coloring
        line = DiscordUtil.aggressiveStrip(line);
        line = MessageUtil.strip(line);

        // do nothing if line is blank before parsing
        if (StringUtils.isBlank(line)) return null;

        // apply regex to line
        for (Map.Entry<Pattern, String> entry : DiscordSRV.getPlugin().getConsoleRegexes().entrySet()) {
            line = entry.getKey().matcher(line).replaceAll(entry.getValue());
            if (StringUtils.isBlank(line)) return null;
        }

        // escape markdown
        line = DiscordUtil.escapeMarkdown(line);

        // trim
        line = line.trim();

        String timestamp = TimeUtil.timeStamp();
        String formattedMessage = PlaceholderUtil.replacePlaceholdersToDiscord(LangUtil.Message.CONSOLE_CHANNEL_LINE.toString())
                .replace("%date%", timestamp)
                .replace("%datetime%", timestamp)
                .replace("%level%", eventLevel)
                .replace("%line%", line);

        this.formatted = formattedMessage;
        return formattedMessage;
    }
}
