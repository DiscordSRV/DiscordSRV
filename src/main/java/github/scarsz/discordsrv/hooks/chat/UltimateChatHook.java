package github.scarsz.discordsrv.hooks.chat;

import br.net.fabiozumbi12.UltimateChat.API.SendChannelMessageEvent;
import br.net.fabiozumbi12.UltimateChat.UCChannel;
import br.net.fabiozumbi12.UltimateChat.UChat;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Arrays;

public class UltimateChatHook implements Listener {

    public UltimateChatHook() {
        PluginUtil.pluginHookIsEnabled("ultimatechat");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(SendChannelMessageEvent event) {
        // make sure chat channel is registered with a destination
        if (DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(event.getChannel().getName()) == null) return;

        // make sure message isn't just blank
        if (StringUtils.isBlank(event.getMessage())) return;

        Player sender = null;
        if (event.getSender() instanceof Player) sender = (Player) event.getSender();

        DiscordSRV.getPlugin().processChatMessage(sender, event.getMessage(), event.getChannel().getName(), false);
    }

    public static void broadcastMessageToChannel(String channel, String message) {
        UCChannel chatChannel = getChannelByCaseInsensitiveName(channel);

        if (chatChannel == null) return; // no suitable channel found

        chatChannel.sendMessage(Bukkit.getServer().getConsoleSender(),
                ChatColor.translateAlternateColorCodes('&', LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                        .replace("%channelcolor%", chatChannel.getColor())
                        .replace("%channelname%", chatChannel.getName())
                        .replace("%channelnickname%", chatChannel.getAlias())
                        .replace("%message%", message)
                )
        );

        PlayerUtil.notifyPlayersOfMentions(player -> Arrays.asList(UChat.chat.getPlayerGroups(player)).contains(channel), message);
    }

    private static UCChannel getChannelByCaseInsensitiveName(String name) {
        for (UCChannel channel : UChat.config.getChannels())
            if (channel.getName().equalsIgnoreCase(name)) return channel;
        return null;
    }

}
