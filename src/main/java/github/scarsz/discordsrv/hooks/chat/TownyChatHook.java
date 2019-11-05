/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.hooks.chat;

import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.TownyChat.channels.Channel;
import com.palmergames.bukkit.TownyChat.events.AsyncChatHookEvent;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import me.vankka.reserializer.minecraft.MinecraftSerializer;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class TownyChatHook implements Listener {

    public TownyChatHook(){
        PluginUtil.pluginHookIsEnabled("townychat");

        Chat instance = (Chat) Bukkit.getPluginManager().getPlugin("TownyChat");
        if (instance == null) { DiscordSRV.info(LangUtil.InternalMessage.TOWNY_NOT_AUTOMATICALLY_ENABLING_CHANNEL_HOOKING); return; }
        List<String> linkedChannels = new LinkedList<>();
        DiscordSRV.getPlugin().getChannels().keySet().forEach(name -> {
            Channel channel = getChannelByCaseInsensitiveName(name);
            if (channel != null) {
                channel.setHooked(true);
                linkedChannels.add(channel.getName());
            }
        });

        if (linkedChannels.size() > 0) {
            DiscordSRV.info((LangUtil.InternalMessage.TOWNY_AUTOMATICALLY_ENABLED_LINKING_FOR_CHANNELS + ": " + String.join(", ", linkedChannels))
                    .replace("{amountofchannels}", String.valueOf(linkedChannels.size()))
            );
        } else {
            DiscordSRV.info(LangUtil.InternalMessage.TOWNY_AUTOMATICALLY_ENABLED_LINKING_FOR_NO_CHANNELS);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(AsyncChatHookEvent event) {
        // make sure chat channel is registered with a destination
        if (DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(event.getChannel().getName()) == null) {
            DiscordSRV.debug("Tried looking up destination Discord channel for Towny channel " + event.getChannel().getName() + " but none found");
            return;
        }

        // make sure message isn't blank
        if (StringUtils.isBlank(event.getMessage())) {
            DiscordSRV.debug("Received blank TownyChat message, not processing");
            return;
        }

        DiscordSRV.getPlugin().processChatMessage(event.getPlayer(), event.getMessage(), event.getChannel().getName(), event.isCancelled());
    }

    public static void broadcastMessageToChannel(String channel, String message) {
        // get instance of TownyChat plugin
        Chat instance = (Chat) Bukkit.getPluginManager().getPlugin("TownyChat");

        // return if TownyChat is disabled
        if (instance == null) return;

        // get the destination channel
        Channel destinationChannel = getChannelByCaseInsensitiveName(channel);

        // return if channel was not available
        if (destinationChannel == null) return;

        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", destinationChannel.getMessageColour())
                .replace("%channelname%", destinationChannel.getName())
                .replace("%channelnickname%", destinationChannel.getChannelTag())
                .replace("%message%", message);

        Consumer<Player> playerConsumer;
        if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer")) {
            TextComponent textComponent = MinecraftSerializer.INSTANCE.serialize(plainMessage);
            playerConsumer = player -> TextAdapter.sendComponent(player, textComponent);
        } else {
            playerConsumer = player -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', plainMessage));
        }

        for (Player player : PlayerUtil.getOnlinePlayers()) {
            if (destinationChannel.isPresent(player.getName())) {
                playerConsumer.accept(player);
            }
        }

        PlayerUtil.notifyPlayersOfMentions(player -> destinationChannel.isPresent(player.getName()), message);
    }

    private static Channel getChannelByCaseInsensitiveName(String name) {
        Chat instance = (Chat) Bukkit.getPluginManager().getPlugin("TownyChat");
        if (instance == null) return null;
        for (Channel townyChannel : instance.getChannelsHandler().getAllChannels().values())
            if (townyChannel.getName().equalsIgnoreCase(name)) return townyChannel;
        return null;
    }

}
