package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 6:12 PM
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class DiscordConsoleListener extends ListenerAdapter {

    private List<String> allowedFileExtensions = new ArrayList<String>() {{
        add("jar");
        //add("zip"); todo support uploading compressed plugins & uncompress
    }};

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        // if message is from null author or self do not process
        if (event.getAuthor() != null && event.getAuthor().getId() != null && DiscordUtil.getJda().getSelfUser().getId() != null && event.getAuthor().getId().equals(DiscordUtil.getJda().getSelfUser().getId())) return;
        // only do anything with the messages if it's in the console channel
        if (DiscordSRV.getPlugin().getConsoleChannel() == null) return;
        if (!event.getChannel().getId().equals(DiscordSRV.getPlugin().getConsoleChannel().getId())) return;

        // handle all attachments
        for (Message.Attachment attachment : event.getMessage().getAttachments()) handleAttachment(event, attachment);

        // get if blacklist acts as whitelist
        boolean DiscordConsoleChannelBlacklistActsAsWhitelist = DiscordSRV.getPlugin().getConfig().getBoolean("DiscordConsoleChannelBlacklistActsAsWhitelist");
        // get banned commands
        List<String> DiscordConsoleChannelBlacklistedCommands = DiscordSRV.getPlugin().getConfig().getStringList("DiscordConsoleChannelBlacklistedCommands");
        // convert to all lower case
        for (int i = 0; i < DiscordConsoleChannelBlacklistedCommands.size(); i++) DiscordConsoleChannelBlacklistedCommands.set(i, DiscordConsoleChannelBlacklistedCommands.get(i).toLowerCase());
        // get base command for manipulation
        String requestedCommand = event.getMessage().getRawContent().trim();
        // get the ass end of commands using this shit minecraft:say
        while (requestedCommand.contains(":")) requestedCommand = requestedCommand.split(":")[1];
        // select the first part of the requested command, being the main part of it we care about
        requestedCommand = requestedCommand.split(" ")[0].toLowerCase(); // *op* person
        // command white/blacklist checking
        boolean allowed = DiscordConsoleChannelBlacklistActsAsWhitelist == DiscordConsoleChannelBlacklistedCommands.contains(requestedCommand);
        // return if command not allowed
        if (!allowed) return;

        // log command to console log file, if this fails the command is not executed for safety reasons unless this is turned off
        try {
            FileUtils.writeStringToFile(
                    new File(DiscordSRV.getPlugin().getConfig().getString("DiscordConsoleChannelUsageLog")),
                    "[" + TimeUtil.timeStamp() + " | ID " + event.getAuthor().getId() + "] " + event.getAuthor().getName() + ": " + event.getMessage().getContent() + System.lineSeparator(),
                    Charset.defaultCharset(),
                    true
            );
        } catch (IOException e) {
            DiscordSRV.error("Error logging console action to " + DiscordSRV.getPlugin().getConfig().getString("DiscordConsoleChannelUsageLog") + ": " + e.getLocalizedMessage());
            if (DiscordSRV.getPlugin().getConfig().getBoolean("CancelConsoleCommandIfLoggingFailed")) return;
        }

        // if server is running paper spigot it has to have it's own little section of code because it whines about timing issues
        Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), () -> Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), event.getMessage().getRawContent()));
    }

    private void handleAttachment(GuildMessageReceivedEvent event, Message.Attachment attachment) {
        String[] attachmentSplit = attachment.getFileName().split("\\.");
        String attachmentExtension = attachmentSplit[attachmentSplit.length - 1];

        if (!allowedFileExtensions.contains(attachmentExtension)) {
            DiscordUtil.sendMessage(event.getChannel(), "Attached file \"" + "\" is of non-plugin extension " + attachmentExtension + ".");
            return;
        }

        String pluginName = attachment.getFileName().substring(0, attachment.getFileName().length() - attachmentExtension.length() - 1);
        File pluginDestination = new File(DiscordSRV.getPlugin().getDataFolder().getParentFile(), attachment.getFileName());

        if (pluginDestination.exists()) {
            // todo do this better
            DiscordUtil.sendMessage(event.getChannel(), "Found existing plugin; unloading & replacing it.");
            PluginUtil.unloadPlugin(Bukkit.getPluginManager().getPlugin(pluginName));

            if (!pluginDestination.delete()) {
                DiscordUtil.sendMessage(event.getChannel(), "Failed to delete the existing jar file. Aborting plugin installation.");
                return;
            }
        }

        DiscordUtil.sendMessage(event.getChannel(), "Downloading plugin " + attachment.getFileName() + "...");
        attachment.download(pluginDestination);
        DiscordUtil.sendMessage(event.getChannel(), "Finished downloading plugin " + attachment.getFileName() + "; initializing it...");
        Plugin loadedPlugin;
        try {
            loadedPlugin = Bukkit.getPluginManager().loadPlugin(pluginDestination);
        } catch (InvalidPluginException | InvalidDescriptionException e) {
            DiscordUtil.sendMessage(event.getChannel(), "Failed loading plugin " + attachment.getFileName() + ": " + e.getLocalizedMessage());
            DiscordSRV.warning("Failed loading plugin " + attachment.getFileName() + ": " + e.getLocalizedMessage());
            pluginDestination.delete();
            return;
        }
        Bukkit.getPluginManager().enablePlugin(loadedPlugin);
        DiscordUtil.sendMessage(event.getChannel(), "Finished installing plugin " + attachment.getFileName() + ".");
    }

}
