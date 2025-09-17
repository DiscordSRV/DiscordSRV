/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.hooks.chat;

import com.pedestriamc.strings.api.StringsProvider;
import com.pedestriamc.strings.api.channel.Channel;
import com.pedestriamc.strings.api.event.channel.ChannelChatEvent;
import com.pedestriamc.strings.api.utlity.SerialComponent;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

public class StringsHook implements ChatHook {

    @Override
    public void broadcastMessageToChannel(String channel, Component message) {
        Channel chatChannel;
        try {
            chatChannel = StringsProvider.get().getChannelLoader().getChannel(channel);
        } catch (Exception e) {
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Attempted to broadcast message to channel \"" + channel + "\" but Strings threw an exception.");
            return;
        }

        if (chatChannel == null) {
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Attempted to broadcast message to channel \"" + channel + "\" but the channel was not found.");
            return;
        }

        Component result = Component.text(LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString())
                .replaceText(replacementConfig("%channelname%", chatChannel.getName()))
                .replaceText(replacementConfig("%channelnickname%", chatChannel.getName()))
                .replaceText(replacementConfig("%channelcolor%", ""))
                .replaceText(replacementConfig("%message%", message));

        String json = GsonComponentSerializer.gson().serialize(result);
        chatChannel.broadcastPlain(new SerialComponent(json));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(ChannelChatEvent event) {
        DiscordSRV.getPlugin().processChatMessage(event.getPlayer(), event.getMessage(), event.getChannel().getName(), event.isCancelled(), event);
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Strings");
    }

    private TextReplacementConfig replacementConfig(String original, String replacement) {
        return TextReplacementConfig.builder()
                .matchLiteral(original)
                .replacement(replacement)
                .build();
    }

    private TextReplacementConfig replacementConfig(String original, Component replacement) {
        return TextReplacementConfig.builder()
                .matchLiteral(original)
                .replacement(replacement)
                .build();
    }

}
