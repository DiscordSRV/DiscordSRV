package github.scarsz.discordsrv.hooks.chat;

import br.com.devpaulo.legendchat.api.Legendchat;
import br.com.devpaulo.legendchat.api.events.ChatMessageEvent;
import br.com.devpaulo.legendchat.channels.types.Channel;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
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
public class LegendChatHook implements Listener {

    public LegendChatHook(){
        DiscordSRV.getPlugin().getHookedPlugins().add("legendchat");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(ChatMessageEvent event) {
        // make sure chat channel is registered with a destination
        if (DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(event.getChannel().getName()) == null) return;

        // make sure message isn't just blank
        if (StringUtils.isBlank(event.getMessage())) return;

        DiscordSRV.getPlugin().processChatMessage(event.getSender().getPlayer(), event.getMessage(), event.getChannel().getName(), event.isCancelled());
    }

    public static void broadcastMessageToChannel(String channelName, String message) {
        Channel chatChannel = getChannelByCaseInsensitiveName(channelName);
        if (chatChannel == null) return; // no suitable channel found
        chatChannel.sendMessage(ChatColor.translateAlternateColorCodes('&', LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", chatChannel.getColor())
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", chatChannel.getNickname())
                .replace("%message%", message)
        ));

        PlayerUtil.notifyPlayersOfMentions(player -> chatChannel.getPlayersWhoCanSeeChannel().contains(player), message);
    }

    private static Channel getChannelByCaseInsensitiveName(String name) {
        for (Channel channel : Legendchat.getChannelManager().getChannels())
            if (channel.getName().equalsIgnoreCase(name)) return channel;
        return null;
    }

}
