package github.scarsz.discordsrv.hooks.chat;

import com.github.ucchyocean.lc.LunaChat;
import com.github.ucchyocean.lc.channel.Channel;
import com.github.ucchyocean.lc.channel.ChannelPlayer;
import com.github.ucchyocean.lc.event.LunaChatChannelChatEvent;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.stream.Collectors;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/25/2017
 * @at 3:50 PM
 */
public class LunaChatHook implements Listener {

    public LunaChatHook() {
        DiscordSRV.getPlugin().getHookedPlugins().add("lunachat");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(LunaChatChannelChatEvent event) {
        // make sure chat channel is registered with a destination
        if (DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(event.getChannel().getName()) == null) return;

        // make sure message isn't just blank
        if (StringUtils.isBlank(event.getNgMaskedMessage())) return;

        // get sender player
        Player player = (event.getPlayer() != null) ? event.getPlayer().getPlayer() : null;

        DiscordSRV.getPlugin().processChatMessage(player, event.getNgMaskedMessage(), event.getChannel().getName(), false);
    }

    public static void broadcastMessageToChannel(String channel, String message) {
        Channel chatChannel = LunaChat.getInstance().getLunaChatAPI().getChannel(channel);
        if (chatChannel == null) return; // no suitable channel found
        chatChannel.sendMessage(null, "", ChatColor.translateAlternateColorCodes('&', LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", chatChannel.getColorCode())
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", (chatChannel.getAlias().equals("")) ? chatChannel.getName() : chatChannel.getAlias() )
                .replace("%message%", message)
        ), true, "Discord");

        PlayerUtil.notifyPlayersOfMentions(player ->
                        chatChannel.getMembers().stream()
                                .map(ChannelPlayer::getPlayer)
                                .collect(Collectors.toList())
                                .contains(player),
                message);
    }

}
