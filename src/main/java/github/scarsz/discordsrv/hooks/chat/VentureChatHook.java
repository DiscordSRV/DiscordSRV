package github.scarsz.discordsrv.hooks.chat;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
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

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/25/2017
 * @at 3:50 PM
 */
public class VentureChatHook implements Listener {

    public VentureChatHook() {
        DiscordSRV.getPlugin().getHookedPlugins().add("venturechat");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void AsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
        // get player that talked
        MineverseChatPlayer chatPlayer = MineverseChatAPI.getOnlineMineverseChatPlayer(event.getPlayer());

        // get channel
        ChatChannel channel = chatPlayer.getCurrentChannel();
        if (chatPlayer.isQuickChat()) channel = chatPlayer.getQuickChannel();

        // make sure player is active
        // if (mcp.isAFK()) return;

        // make sure chat is in a channel
        if (channel == null) return;

        // make sure chat is not in party chat
        if (chatPlayer.isPartyChat() && !chatPlayer.isQuickChat()) return;

        // make sure chat isn't a direct message
        if (event.getMessage().startsWith("@")) return;

        // make sure user isn't muted in channel
        if (chatPlayer.isMuted(channel.getName())) return;

        // make sure player has permission to talk in channel
        if (channel.hasPermission() && !chatPlayer.getPlayer().hasPermission(channel.getPermission())) return;

        // filter chat if bad words filter is on for channel and player
        String msg = event.getMessage();
        if (channel.isFiltered() && chatPlayer.hasFilter()) msg = MineverseChat.ccInfo.FilterChat(msg);

        DiscordSRV.getPlugin().processChatMessage(event.getPlayer(), msg, channel.getName(), event.isCancelled());
    }

    public static void broadcastMessageToChannel(String channel, String message) {
        if (channel.equalsIgnoreCase("global")) channel = "Global";
        ChatChannel chatChannel = MineverseChat.ccInfo.getChannelInfo(channel); // case in-sensitive by default(?)

        if (chatChannel == null) {
            DiscordSRV.debug("Attempted to broadcast message to channel \"" + channel + "\" but got null channel info; aborting message");
            return;
        }

        List<MineverseChatPlayer> playersToNotify = MineverseChat.onlinePlayers.stream().filter(p -> p.getListening().contains(chatChannel.getName())).collect(Collectors.toList());

        for (MineverseChatPlayer player : playersToNotify) {
            // filter chat if bad words filter is on for channel and player
            String msg = message;
            if (chatChannel.isFiltered() && player.hasFilter()) msg = MineverseChat.ccInfo.FilterChat(msg);

            player.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                    .replace("%channelcolor%", ChatColor.valueOf(chatChannel.getColor().toUpperCase()).toString())
                    .replace("%channelname%", chatChannel.getName())
                    .replace("%channelnickname%", chatChannel.getAlias())
                    .replace("%message%", msg))
            );
        }

        PlayerUtil.notifyPlayersOfMentions(player ->
                        playersToNotify.stream()
                                .map(MineverseChatPlayer::getPlayer)
                                .collect(Collectors.toList())
                                .contains(player),
                message);
    }

}
