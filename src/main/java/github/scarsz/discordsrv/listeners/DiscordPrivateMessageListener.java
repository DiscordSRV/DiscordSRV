package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordPrivateMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 6:09 PM
 */
public class DiscordPrivateMessageListener extends ListenerAdapter {

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        DiscordSRV.api.callEvent(new DiscordPrivateMessageReceivedEvent(event));

        String reply = DiscordSRV.getPlugin().getAccountLinkManager().process(event.getMessage().getRawContent(), event.getAuthor().getId());
        if (reply != null) event.getChannel().sendMessage(reply).queue();
    }

}
