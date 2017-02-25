package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;

import java.io.File;
import java.util.ArrayList;
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
        // only do anything with the messages if it's in the console channel
        if (DiscordSRV.getPlugin().getConsoleChannel() == null) return;
        if (!event.getChannel().getId().equals(DiscordSRV.getPlugin().getConsoleChannel().getId())) return;

        // handle all attachments
        for (Message.Attachment attachment : event.getMessage().getAttachments()) handleAttachment(event, attachment);

        //todo
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
        try {
            Bukkit.getPluginManager().loadPlugin(pluginDestination);
        } catch (InvalidPluginException | InvalidDescriptionException e) {
            DiscordUtil.sendMessage(event.getChannel(), "Failed loading plugin " + attachment.getFileName() + ": " + e.getLocalizedMessage());
            DiscordSRV.warning("Failed loading plugin " + attachment.getFileName() + ": " + e.getLocalizedMessage());
            pluginDestination.delete();
            return;
        }
        Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin(pluginName));
        DiscordUtil.sendMessage(event.getChannel(), "Finished installing plugin " + attachment.getFileName() + ".");
    }

}
