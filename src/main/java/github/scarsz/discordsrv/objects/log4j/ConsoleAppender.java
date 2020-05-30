/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.objects.log4j;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

@Plugin(name = "DiscordSRV-ConsoleChannel", category = "Core", elementType = "appender", printObject = true)
public class ConsoleAppender extends AbstractAppender {

    private static final PatternLayout PATTERN_LAYOUT;
    static {
        Method createLayoutMethod = null;
        for (Method method : PatternLayout.class.getMethods()) {
            if (method.getName().equals("createLayout")) {
                createLayoutMethod = method;
            }
        }
        if (createLayoutMethod == null) {
            DiscordSRV.error("Failed to reflectively find the Log4j createLayout method. The console appender is not going to function.");
            PATTERN_LAYOUT = null;
        } else {
            Object[] args = new Object[createLayoutMethod.getParameterCount()];
            args[0] = "[%d{HH:mm:ss} %level]: %msg";
            if (args.length == 9) {
                // log4j 2.1
                args[5] = true;
                args[6] = true;
            }

            PatternLayout createdLayout = null;
            try {
                createdLayout = (PatternLayout) createLayoutMethod.invoke(null, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                DiscordSRV.error("Failed to reflectively invoke the Log4j createLayout method. The console appender is not going to function.");
                e.printStackTrace();
            }
            PATTERN_LAYOUT = createdLayout;
        }
    }

    public ConsoleAppender() {
        super("DiscordSRV-ConsoleChannel", null, PATTERN_LAYOUT, false);

        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addAppender(this);
    }

    public void shutdown() {
        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.removeAppender(this);
    }

    @Override
    public boolean isStarted() {
        return PATTERN_LAYOUT != null;
    }

    @Override
    public void append(LogEvent e) {
        // return if console channel isn't available
        if (DiscordSRV.getPlugin().getConsoleChannel() == null) return;

        // return if this is not an okay level to send
        boolean isAnOkayLevel = false;
        for (String consoleLevel : DiscordSRV.config().getStringList("DiscordConsoleChannelLevels")) if (consoleLevel.toLowerCase().equals(e.getLevel().name().toLowerCase())) isAnOkayLevel = true;
        if (!isAnOkayLevel) return;

        String line = e.getMessage().getFormattedMessage();

        // remove coloring
        line = DiscordUtil.aggressiveStrip(line);

        // do nothing if line is blank before parsing
        if (StringUtils.isBlank(line)) return;

        // apply regex to line
        line = applyRegex(line);

        // do nothing if line is blank after parsing
        if (StringUtils.isBlank(line)) return;

        // escape markdown
        line = DiscordUtil.escapeMarkdown(line);

        // apply formatting
        line = LangUtil.Message.CONSOLE_CHANNEL_LINE.toString()
                .replace("%date%", TimeUtil.timeStamp())
                .replace("%level%", e.getLevel().name().toUpperCase())
                .replace("%line%", line)
        ;

        // if line contains a blocked phrase don't send it
        boolean whitelist = DiscordSRV.config().getBoolean("DiscordConsoleChannelDoNotSendPhrasesActsAsWhitelist");
        List<String> phrases = DiscordSRV.config().getStringList("DiscordConsoleChannelDoNotSendPhrases");
        for (String phrase : phrases) {
            boolean contained = StringUtils.containsIgnoreCase(line, phrase);
            if (contained != whitelist) return;
        }

        // queue final message
        DiscordSRV.getPlugin().getConsoleMessageQueue().add(line);
    }

    private String applyRegex(String input) {
        String regexFilter = DiscordSRV.config().getString("DiscordConsoleChannelRegexFilter");
        if (StringUtils.isNotBlank(regexFilter)) {
            return input.replaceAll(regexFilter, DiscordSRV.config().getString("DiscordConsoleChannelRegexReplacement"));
        } else {
            return input;
        }
    }

}
