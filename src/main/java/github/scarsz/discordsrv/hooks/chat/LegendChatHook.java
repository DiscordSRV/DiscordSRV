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

import br.com.devpaulo.legendchat.api.Legendchat;
import br.com.devpaulo.legendchat.api.events.ChatMessageEvent;
import br.com.devpaulo.legendchat.channels.types.Channel;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

public class LegendChatHook implements ChatHook {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(ChatMessageEvent event) {
        // make sure message isn't just blank
        if (StringUtils.isBlank(event.getMessage())) return;

        DiscordSRV.getPlugin().processChatMessage(event.getSender().getPlayer(), event.getMessage(), event.getChannel().getName(), event.isCancelled());
    }

    @Override
    public void broadcastMessageToChannel(String channelName, Component message) {
        Channel chatChannel = getChannelByCaseInsensitiveName(channelName);
        if (chatChannel == null) return; // no suitable channel found
        String legacy = MessageUtil.toLegacy(message);

        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", chatChannel.getNickname())
                .replace("%message%", legacy)
                .replace("%channelcolor%", MessageUtil.toLegacy(MessageUtil.toComponent(MessageUtil.translateLegacy(chatChannel.getColor()))));

        String translatedMessage = MessageUtil.translateLegacy(plainMessage);
        chatChannel.sendMessage(translatedMessage);
        PlayerUtil.notifyPlayersOfMentions(player -> chatChannel.getPlayersWhoCanSeeChannel().contains(player), legacy);
    }

    private static Channel getChannelByCaseInsensitiveName(String name) {
        for (Channel channel : Legendchat.getChannelManager().getChannels())
            if (channel.getName().equalsIgnoreCase(name)) return channel;
        return null;
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("LegendChat");
    }

}
