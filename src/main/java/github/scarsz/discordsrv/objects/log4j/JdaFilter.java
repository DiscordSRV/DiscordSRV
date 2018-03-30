/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2018 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.objects.log4j;

import github.scarsz.discordsrv.DiscordSRV;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

import java.util.Arrays;
import java.util.List;

public class JdaFilter implements Filter {

    private static final List<Level> normalLogLevels = Arrays.asList(Level.INFO, Level.WARN, Level.ERROR);

    public Result check(Logger logger, Level level, String message) {
        // only listen for JDA logs
        if (!logger.getName().startsWith("github.scarsz.discordsrv.dependencies.jda")) return Result.NEUTRAL;

        if (normalLogLevels.contains(level)) {
            switch (level.name()) {
                case "INFO": DiscordSRV.info("[JDA] " + message); break;
                case "WARN": DiscordSRV.warning("[JDA] " + message); break;
                case "ERROR": DiscordSRV.error("[JDA] " + message); break;
            }
        } else if (DiscordSRV.config().getBoolean("DebugJDA")) {
            DiscordSRV.debug(message);
        }

        // all JDA messages should be denied because we handle them ourselves
        return Result.DENY;
    }

    @Override
    public Result filter(LogEvent logEvent) {
        return check((Logger) LogManager.getLogger(logEvent.getLoggerName()), logEvent.getLevel(), logEvent.getMessage().getFormattedMessage());
    }
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object... parameters) {
        return check(logger, level, message);
    }
    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object message, Throwable throwable) {
        return check(logger, level, message.toString());
    }
    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message message, Throwable throwable) {
        return check(logger, level, message.getFormattedMessage());
    }

    public void start() {}
    public void stop() {}
    public boolean isStarted() {
        return true;
    }
    public boolean isStopped() {
        return false;
    }
    @Override
    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }
    @Override
    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

}
