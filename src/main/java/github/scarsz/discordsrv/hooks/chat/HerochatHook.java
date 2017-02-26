package github.scarsz.discordsrv.hooks.chat;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import github.scarsz.discordsrv.DiscordSRV;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/25/2017
 * @at 3:50 PM
 */
public class HerochatHook implements Listener {

    public HerochatHook() {
        DiscordSRV.getPlugin().getHookedPlugins().add("herochat");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(ChannelChatEvent event) {
        // make sure chat channel is registered with a destination
        if (DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(event.getChannel().getName()) == null) return;

        // make sure message isn't just blank
        if (StringUtils.isBlank(event.getMessage())) return;

        DiscordSRV.getPlugin().processChatMessage(event.getSender().getPlayer(), event.getMessage(), event.getChannel().getName(), event.getResult() != Chatter.Result.ALLOWED);
    }

    public static void broadcastMessageToChannel(String channel, String message, String rawMessage) {
        Channel chatChannel = getChannelByCaseInsensitiveName(channel);
        if (chatChannel == null) return; // no suitable channel found
        chatChannel.sendRawMessage(ChatColor.translateAlternateColorCodes('&', DiscordSRV.getPlugin().getConfig().getString("ChatChannelHookMessageFormat")
                .replace("%channelcolor%", chatChannel.getColor().toString())
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", chatChannel.getNick())
                .replace("%message%", message))
        );
    }

    private static Channel getChannelByCaseInsensitiveName(String name) {
        for (Channel channel : Herochat.getChannelManager().getChannels())
            if (channel.getName().equalsIgnoreCase(name)) return channel;
        return null;
    }

}
