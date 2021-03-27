/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.hooks.chat;

import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.TownyChat.channels.Channel;
import com.palmergames.bukkit.TownyChat.events.AsyncChatHookEvent;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import java.util.LinkedList;
import java.util.List;

public class TownyChatHook implements ChatHook {

    public TownyChatHook() {
        reload();
    }

    public void reload() {
        if (!isEnabled()) return;

        Chat instance = (Chat) Bukkit.getPluginManager().getPlugin("TownyChat");
        if (instance == null) {
            DiscordSRV.info("Could not automatically hook TownyChat channels");
            return;
        }

        List<String> linkedChannels = new LinkedList<>();
        List<String> availableChannels = new LinkedList<>();
        DiscordSRV.getPlugin().getChannels().keySet().forEach(name -> {
            Channel channel = getChannelByCaseInsensitiveName(name);
            if (channel != null) {
                channel.setHooked(true);
                linkedChannels.add(channel.getName());
            }
        });
        for (Channel channel : instance.getChannelsHandler().getAllChannels().values()) {
            availableChannels.add(channel.getName());
        }

        if (!linkedChannels.isEmpty()) {
            DiscordSRV.info("Marked the following TownyChat channels as hooked: " + (String.join(", ", linkedChannels)) + ". Available channels: " + String.join(", ", availableChannels));
        } else {
            DiscordSRV.info("No TownyChat channels were marked as hooked. Available channels: " + String.join(", ", availableChannels));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(AsyncChatHookEvent event) {
        // make sure chat channel is registered with a destination
        if (DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(event.getChannel().getName()) == null) {
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Tried looking up destination Discord channel for Towny channel " + event.getChannel().getName() + " but none found");
            return;
        }

        // make sure message isn't blank
        if (StringUtils.isBlank(event.getMessage())) {
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received blank TownyChat message, not processing");
            return;
        }

        DiscordSRV.getPlugin().processChatMessage(event.getPlayer(), event.getMessage(), event.getChannel().getName(), event.isCancelled());
    }

    @Override
    public void broadcastMessageToChannel(String channel, Component message) {
        // get instance of TownyChat plugin
        Chat instance = (Chat) Bukkit.getPluginManager().getPlugin("TownyChat");

        // return if TownyChat is disabled
        if (instance == null) return;

        // get the destination channel
        Channel destinationChannel = getChannelByCaseInsensitiveName(channel);

        // return if channel was not available
        if (destinationChannel == null) return;
        String legacy = MessageUtil.toLegacy(message);

        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", destinationChannel.getMessageColour() != null ? destinationChannel.getMessageColour() : "")
                .replace("%channelname%", destinationChannel.getName())
                .replace("%channelnickname%", destinationChannel.getChannelTag() != null ? destinationChannel.getChannelTag() : "")
                .replace("%message%", legacy);

        String translatedMessage = MessageUtil.translateLegacy(plainMessage);
        for (Player player : PlayerUtil.getOnlinePlayers()) {
            if (destinationChannel.isPresent(player.getName())) {
                MessageUtil.sendMessage(player, translatedMessage);
            }
        }

        PlayerUtil.notifyPlayersOfMentions(player -> destinationChannel.isPresent(player.getName()), legacy);
    }

    private static Channel getChannelByCaseInsensitiveName(String name) {
        Chat instance = (Chat) Bukkit.getPluginManager().getPlugin("TownyChat");
        if (instance == null) return null;
        for (Channel townyChannel : instance.getChannelsHandler().getAllChannels().values())
            if (townyChannel.getName().equalsIgnoreCase(name)) return townyChannel;
        return null;
    }

    public static String getMainChannelName() {
        Chat instance = (Chat) Bukkit.getPluginManager().getPlugin("TownyChat");
        if (instance == null) return null;
        Channel channel = instance.getChannelsHandler().getDefaultChannel();
        if (channel == null) return null;
        return channel.getName();
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("TownyChat");
    }

}
