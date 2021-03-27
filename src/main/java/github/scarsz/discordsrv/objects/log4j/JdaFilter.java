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

package github.scarsz.discordsrv.objects.log4j;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

public class JdaFilter implements Filter {

    public Result check(String loggerName, Level level, String message, Throwable throwable) {
        // only listen for JDA logs
        if (!loggerName.startsWith("github.scarsz.discordsrv.dependencies.jda")) return Result.NEUTRAL;

        switch (level.name()) {
            case "INFO": DiscordSRV.info("[JDA] " + message); break;
            case "WARN":
                if (message.contains("Encountered 429")) {
                    DiscordSRV.debug(message);
                    break;
                }

                DiscordSRV.warning("[JDA] " + message);
                break;
            case "ERROR":
                if (message.contains("Requester timed out while executing a request")) {
                    DiscordSRV.error("[JDA] " + message + ". This is either a issue on Discord's end (https://discordstatus.com) or with your server's connection");
                    DiscordSRV.debug(ExceptionUtils.getStackTrace(throwable));
                    break;
                }

                if (throwable != null) {
                    DiscordSRV.error("[JDA] " + message + "\n" + ExceptionUtils.getStackTrace(throwable));
                } else {
                    DiscordSRV.error("[JDA] " + message);
                }
                break;
            default: if (Debug.JDA.isVisible()) DiscordSRV.debug("[JDA] " + message);
        }

        // all JDA messages should be denied because we handle them ourselves
        return Result.DENY;
    }

    @Override
    public Result filter(LogEvent logEvent) {
        return check(
                logEvent.getLoggerName(),
                logEvent.getLevel(),
                logEvent.getMessage()
                        .getFormattedMessage(),
                logEvent.getThrown());
    }
    @Override
    public Result filter(Logger logger, Level level, Marker marker, String message, Object... parameters) {
        return check(
                logger.getName(),
                level,
                message,
                null);
    }
    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object message, Throwable throwable) {
        return check(
                logger.getName(),
                level,
                message.toString(),
                throwable);
    }
    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message message, Throwable throwable) {
        return check(
                logger.getName(),
                level,
                message.getFormattedMessage(),
                throwable);
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
