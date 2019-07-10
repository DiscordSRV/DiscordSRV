/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class DiscordConsoleListener extends ListenerAdapter {

    private List<String> allowedFileExtensions = new ArrayList<String>() {{
        add("jar");
        //add("zip"); todo support uploading compressed plugins & uncompress
    }};

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        // if message is from null author or self do not process
        if (event.getAuthor() == null || event.getAuthor().getId() == null || DiscordUtil.getJda().getSelfUser().getId() == null || event.getAuthor().getId().equals(DiscordUtil.getJda().getSelfUser().getId())) return;
        // only do anything with the messages if it's in the console channel
        if (DiscordSRV.getPlugin().getConsoleChannel() == null || !event.getChannel().getId().equals(DiscordSRV.getPlugin().getConsoleChannel().getId())) return;

        // handle all attachments
        for (Message.Attachment attachment : event.getMessage().getAttachments()) handleAttachment(event, attachment);

        // get if blacklist acts as whitelist
        boolean DiscordConsoleChannelBlacklistActsAsWhitelist = DiscordSRV.config().getBoolean("DiscordConsoleChannelBlacklistActsAsWhitelist");
        // get banned commands
        List<String> DiscordConsoleChannelBlacklistedCommands = DiscordSRV.config().getStringList("DiscordConsoleChannelBlacklistedCommands");
        // convert to all lower case
        for (int i = 0; i < DiscordConsoleChannelBlacklistedCommands.size(); i++) DiscordConsoleChannelBlacklistedCommands.set(i, DiscordConsoleChannelBlacklistedCommands.get(i).toLowerCase());
        // get base command for manipulation
        String requestedCommand = event.getMessage().getContentRaw().trim();
        // get the ass end of commands using this shit minecraft:say
        while (requestedCommand.contains(":")) requestedCommand = requestedCommand.split(":", 2)[1];
        // select the first part of the requested command, being the main part of it we care about
        requestedCommand = requestedCommand.split(" ")[0].toLowerCase(); // *op* person
        // command white/blacklist checking
        boolean allowed = DiscordConsoleChannelBlacklistActsAsWhitelist == DiscordConsoleChannelBlacklistedCommands.contains(requestedCommand);
        // return if command not allowed
        if (!allowed) return;

        // log command to console log file, if this fails the command is not executed for safety reasons unless this is turned off
        try {
            String fileName = DiscordSRV.config().getString("DiscordConsoleChannelUsageLog");
            if (StringUtils.isNotBlank(fileName)) {
                FileUtils.writeStringToFile(
                        new File(fileName),
                        "[" + TimeUtil.timeStamp() + " | ID " + event.getAuthor().getId() + "] " + event.getAuthor().getName() + ": " + event.getMessage().getContentRaw() + System.lineSeparator(),
                        Charset.forName("UTF-8"),
                        true
                );
            }

        } catch (IOException e) {
            DiscordSRV.error(LangUtil.InternalMessage.ERROR_LOGGING_CONSOLE_ACTION + " " + DiscordSRV.config().getString("DiscordConsoleChannelUsageLog") + ": " + e.getMessage());
            if (DiscordSRV.config().getBoolean("CancelConsoleCommandIfLoggingFailed")) return;
        }

        // if server is running paper spigot it has to have it's own little section of code because it whines about timing issues
        Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), event.getMessage().getContentRaw()));
    }

    private void handleAttachment(GuildMessageReceivedEvent event, Message.Attachment attachment) {
        String[] attachmentSplit = attachment.getFileName().split("\\.");
        String attachmentExtension = attachmentSplit[attachmentSplit.length - 1];

        if (!allowedFileExtensions.contains(attachmentExtension)) {
            DiscordUtil.sendMessage(event.getChannel(), "Attached file \"" + attachment.getFileName() + "\" is of non-plugin extension " + attachmentExtension + ".");
            return;
        }

        File pluginDestination = new File(DiscordSRV.getPlugin().getDataFolder().getParentFile(), attachment.getFileName());

        if (pluginDestination.exists()) {
            String pluginName = null;
            try {
                ZipFile jarZipFile = new ZipFile(pluginDestination);
                Enumeration<? extends ZipEntry> entries = jarZipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    pluginName = getPluginName(pluginName, jarZipFile, entry);
                }
                jarZipFile.close();
            } catch (IOException e) {
                DiscordUtil.sendMessage(event.getChannel(), "Failed loading plugin " + attachment.getFileName() + ": " + e.getMessage());
                DiscordSRV.warning(LangUtil.InternalMessage.FAILED_LOADING_PLUGIN + " " + attachment.getFileName() + ": " + e.getMessage());
                pluginDestination.delete();
                return;
            }
            if (pluginName == null) {
                DiscordUtil.sendMessage(event.getChannel(), "Attached file \"" + attachment.getFileName() + "\" either did not have a plugin.yml or it's plugin.yml did not contain it's name.");
                DiscordSRV.warning(LangUtil.InternalMessage.FAILED_LOADING_PLUGIN + " " + attachment.getFileName() + ": Attached file \"" + attachment.getFileName() + "\" either did not have a plugin.yml or it's plugin.yml did not contain it's name.");
                pluginDestination.delete();
                return;
            }

            Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);

            PluginUtil.unloadPlugin(plugin);
            if (!pluginDestination.delete()) {
                DiscordUtil.sendMessage(event.getChannel(), "Failed deleting existing plugin");
                return;
            }
        }

        // download plugin jar from Discord
        attachment.download(pluginDestination);

        String pluginName = null;
        try {
            ZipFile jarZipFile = new ZipFile(pluginDestination);
            while (jarZipFile.entries().hasMoreElements()) {
                ZipEntry entry = jarZipFile.entries().nextElement();
                pluginName = getPluginName(pluginName, jarZipFile, entry);
            }
            jarZipFile.close();
        } catch (IOException e) {
            DiscordUtil.sendMessage(event.getChannel(), "Failed loading plugin " + attachment.getFileName() + ": " + e.getMessage());
            DiscordSRV.warning(LangUtil.InternalMessage.FAILED_LOADING_PLUGIN + " " + attachment.getFileName() + ": " + e.getMessage());
            pluginDestination.delete();
            return;
        }

        if (pluginName == null) {
            DiscordUtil.sendMessage(event.getChannel(), "Attached file \"" + attachment.getFileName() + "\" either did not have a plugin.yml or it's plugin.yml did not contain it's name.");
            DiscordSRV.warning(LangUtil.InternalMessage.FAILED_LOADING_PLUGIN + " " + attachment.getFileName() + ": Attached file \"" + attachment.getFileName() + "\" either did not have a plugin.yml or it's plugin.yml did not contain it's name.");
            pluginDestination.delete();
            return;
        }

        Plugin loadedPlugin = Bukkit.getPluginManager().getPlugin(pluginName);

        if (loadedPlugin != null) {
            Bukkit.getPluginManager().disablePlugin(loadedPlugin);
            PluginUtil.unloadPlugin(loadedPlugin);
        }
        loadedPlugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (loadedPlugin != null) {
            DiscordUtil.sendMessage(event.getChannel(), "Attached file \"" + attachment.getFileName() + "\" either did not have a plugin.yml or it's plugin.yml did not contain it's name.");
            DiscordSRV.warning(LangUtil.InternalMessage.FAILED_LOADING_PLUGIN + " " + attachment.getFileName() + ": Attached file \"" + attachment.getFileName() + "\" either did not have a plugin.yml or it's plugin.yml did not contain it's name.");
            pluginDestination.delete();
            return;
        }

        try {
            loadedPlugin = Bukkit.getPluginManager().loadPlugin(pluginDestination);
        } catch (InvalidPluginException | InvalidDescriptionException e) {
            DiscordUtil.sendMessage(event.getChannel(), "Failed loading plugin " + attachment.getFileName() + ": " + e.getMessage());
            DiscordSRV.warning(LangUtil.InternalMessage.FAILED_LOADING_PLUGIN + " " + attachment.getFileName() + ": " + e.getMessage());
            pluginDestination.delete();
            return;
        }

        if (loadedPlugin != null) {
            Bukkit.getPluginManager().enablePlugin(loadedPlugin);
        }

        DiscordUtil.sendMessage(event.getChannel(), "Finished installing plugin " + attachment.getFileName() + " " + loadedPlugin + ".");
    }

    private String getPluginName(String pluginName, ZipFile jarZipFile, ZipEntry entry) throws IOException {
        if (!entry.getName().equalsIgnoreCase("plugin.yml")) return pluginName;
        BufferedReader reader = new BufferedReader(new InputStreamReader(jarZipFile.getInputStream(entry)));
        for (String line : reader.lines().collect(Collectors.toList()))
            if (line.trim().startsWith("name:"))
                pluginName = line.replace("name:", "").trim();
        return pluginName;
    }
}
