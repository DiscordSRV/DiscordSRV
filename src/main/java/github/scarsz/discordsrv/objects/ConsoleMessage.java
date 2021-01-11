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

import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import lombok.Data;

@Data
public class ConsoleMessage {
    private final String timestamp;
    private final String level;
    private final String line;

    @Override
    public String toString() {
        return PlaceholderUtil.replacePlaceholdersToDiscord(LangUtil.Message.CONSOLE_CHANNEL_LINE.toString())
                .replace("%date%", timestamp)
                .replace("%level%", level)
                .replace("%line%", line);
    }
}
