package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.util.DebugUtil;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 6:12 PM
 */
public class DiscordDebugListener extends ListenerAdapter {

    private List<String> authorized = new ArrayList<String>() {{
        add("95088531931672576"); // Scarsz
        add("142968127829835777"); // Androkai
    }};

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // make sure it's not some random fucknut like mepeisen
        if (!authorized.contains(event.getAuthor().getId()) && // one of the developers
                (event.isFromType(ChannelType.TEXT) && !event.getGuild().getOwner().getUser().getId().equals(event.getAuthor().getId())) // guild owner
        ) return;

        // make sure the author meant to trigger this
        if (!event.getMessage().getRawContent().equalsIgnoreCase("discorddebug") &&
                !event.getMessage().getRawContent().equalsIgnoreCase("discordsrvdebug")) return;

        DiscordUtil.deleteMessage(event.getMessage());
        DiscordUtil.privateMessage(event.getAuthor(), DebugUtil.run(event.getAuthor().toString()));
    }

}
