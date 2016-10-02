package com.scarsz.discordsrv.hooks.chat;

import com.scarsz.discordsrv.DiscordSRV;
import mineverse.Aust1n46.chat.MineverseChat;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import mineverse.Aust1n46.chat.channel.ChatChannel;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.stream.Collectors;

public class VentureChatHook implements Listener {

    public VentureChatHook() {
        DiscordSRV.hookedPlugins.add("venturechat");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        MineverseChatPlayer mcp = MineverseChatAPI.getOnlineMineverseChatPlayer(event.getPlayer());
        ChatChannel eventChannel = mcp.getCurrentChannel();
        if(mcp.isQuickChat()) eventChannel = mcp.getQuickChannel();

        // make sure player is active
        if (mcp.isAFK()) return;

        // make sure chat is in a channel
        if (mcp.hasConversation() && !mcp.isQuickChat()) return;

        // make sure chat is not in party chat
        if (mcp.isPartyChat() && !mcp.isQuickChat()) return;

        // make sure chat isn't a direct message
        if (event.getMessage().startsWith("@")) return;

        // make sure user isn't muted in channel
        if (mcp.isMuted(eventChannel.getName())) return;

        // make sure player has permission to talk in channel
        if (eventChannel.hasPermission() && !mcp.getPlayer().hasPermission(eventChannel.getPermission())) return;

        // filter chat if bad words filter is on for channel and player
        String msg = event.getMessage();
        if (eventChannel.isFiltered() && mcp.hasFilter()) msg = MineverseChat.ccInfo.FilterChat(msg);

        DiscordSRV.processChatEvent(event.isCancelled(), event.getPlayer(), msg, eventChannel.getName());
    }

    public static void broadcastMessageToChannel(String channel, String message, String rawMessage) {
        List<MineverseChatPlayer> playersToNotify = MineverseChat.onlinePlayers.stream().filter(p -> p.getListening().contains(channel)).collect(Collectors.toList());
        ChatChannel chatChannel = MineverseChat.ccInfo.getChannelInfo(channel);

        for (MineverseChatPlayer player : playersToNotify) {
            String msg = message;
            // filter chat if bad words filter is on for channel and player
            if (chatChannel.isFiltered() && player.hasFilter()) msg = MineverseChat.ccInfo.FilterChat(msg);

            player.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', DiscordSRV.plugin.getConfig().getString("ChatChannelHookMessageFormat")
                    .replace("%channelcolor%", ChatColor.valueOf(chatChannel.getColor().toUpperCase()).toString())
                    .replace("%channelname%", chatChannel.getName())
                    .replace("%channelnickname%", chatChannel.getAlias())
                    .replace("%message%", msg)));
        }

        // notify players
        DiscordSRV.notifyPlayersOfMentions(playersToNotify.stream().map(MineverseChatPlayer::getPlayer).collect(Collectors.toList()), rawMessage);
    }

}
