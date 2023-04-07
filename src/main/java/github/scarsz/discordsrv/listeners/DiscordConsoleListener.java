/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordConsoleCommandPostProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordConsoleCommandPreProcessEvent;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class DiscordConsoleListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        // check if the server hasn't started yet but someone still tried to run a command...
        if (DiscordUtil.getJda() == null) return;
        // if message is from null author or self do not process
        if (event.getAuthor().getId().equals(DiscordUtil.getJda().getSelfUser().getId())) return;
        // only do anything with the messages if it's in the console channel
        if (DiscordSRV.getPlugin().getConsoleChannel() == null || !event.getChannel().getId().equals(DiscordSRV.getPlugin().getConsoleChannel().getId()))
            return;
        // block bots
        if (DiscordSRV.config().getBoolean("DiscordConsoleChannelBlockBots") && event.getAuthor().isBot()) {
            DiscordSRV.debug(Debug.UNCATEGORIZED, "Received a message from a bot in the console channel, but DiscordConsoleChannelBlockBots is enabled");
            return;
        }

        boolean isWhitelist = DiscordSRV.config().getBoolean("DiscordConsoleChannelBlacklistActsAsWhitelist");
        List<String> blacklistedCommands = DiscordSRV.config().getStringList("DiscordConsoleChannelBlacklistedCommands");

        for (int i = 0; i < blacklistedCommands.size(); i++) blacklistedCommands.set(i, blacklistedCommands.get(i).toLowerCase());

        String requestedCommand = event.getMessage().getContentRaw().trim().split(" ")[0].toLowerCase();
        requestedCommand = requestedCommand.substring(requestedCommand.lastIndexOf(":") + 1);
        if (isWhitelist != blacklistedCommands.contains(requestedCommand)) return;

        // log command to console log file, if this fails the command is not executed for safety reasons unless this is turned off
        File logFile = DiscordSRV.getPlugin().getLogFile();
        if (logFile != null) {
            try {
                FileUtils.writeStringToFile(
                        logFile,
                        "[" + TimeUtil.timeStamp() + " | ID " + event.getAuthor().getId() + "] " + event.getAuthor().getName() + ": " + event.getMessage().getContentRaw() + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        true
                );
            } catch (IOException e) {
                DiscordSRV.error(LangUtil.InternalMessage.ERROR_LOGGING_CONSOLE_ACTION + " " + logFile.getAbsolutePath() + ": " + e.getMessage());
                if (DiscordSRV.config().getBoolean("CancelConsoleCommandIfLoggingFailed")) return;
            }
        }

        DiscordConsoleCommandPreProcessEvent consoleEvent = DiscordSRV.api.callEvent(new DiscordConsoleCommandPreProcessEvent(event, event.getMessage().getContentRaw(), true));

        // stop the command from being run if an API user cancels the event
        if (consoleEvent.isCancelled()) return;

        DiscordSRV.getPlugin().getConsoleAppender().dumpStack();
        SchedulerUtil.runTask(DiscordSRV.getPlugin(), () ->
                Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), consoleEvent.getCommand()));

        DiscordSRV.api.callEvent(new DiscordConsoleCommandPostProcessEvent(event, consoleEvent.getCommand(), true));
    }
}
