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

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import me.vankka.reserializer.minecraft.MinecraftSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.List;
import java.util.stream.Collectors;

public class HerochatHook implements Listener {

    public HerochatHook() {
        PluginUtil.pluginHookIsEnabled("herochat");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(ChannelChatEvent event) {
        // make sure chat channel is registered with a destination
        if (DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(event.getChannel().getName()) == null) return;

        // make sure message isn't just blank
        if (StringUtils.isBlank(event.getMessage())) return;

        DiscordSRV.getPlugin().processChatMessage(event.getSender().getPlayer(), event.getMessage(), event.getChannel().getName(), event.getResult() != Chatter.Result.ALLOWED);
    }

    public static void broadcastMessageToChannel(String channel, String message) {
        Channel chatChannel = getChannelByCaseInsensitiveName(channel);
        if (chatChannel == null) return; // no suitable channel found

        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", chatChannel.getColor().toString())
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", chatChannel.getNick())
                .replace("%message%", message);

        if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer")) {
            chatChannel.sendRawMessage(LegacyComponentSerializer.INSTANCE.serialize(MinecraftSerializer.INSTANCE.serialize(plainMessage)));
        } else {
            chatChannel.sendRawMessage(ChatColor.translateAlternateColorCodes('&', plainMessage));
        }

        PlayerUtil.notifyPlayersOfMentions(player ->
                        chatChannel.getMembers().stream()
                                .map(Chatter::getPlayer)
                                .collect(Collectors.toList())
                                .contains(player),
                message);
    }

    private static Channel getChannelByCaseInsensitiveName(String name) {
        List<Channel> channels = Herochat.getChannelManager().getChannels();

        if (channels.size() > 0) {
            for (Channel channel : Herochat.getChannelManager().getChannels()) {
                DiscordSRV.debug("\"" + channel.getName() + "\" equalsIgnoreCase \"" + name + "\" == " + channel.getName().equalsIgnoreCase(name));
                if (channel.getName().equalsIgnoreCase(name)) {
                    return channel;
                }
            }
            DiscordSRV.debug("No matching Herochat channels for name \"" + name + "\"");
        } else {
            DiscordSRV.debug("Herochat's channel manager returned no registered channels");
        }
        return null;
    }

}
