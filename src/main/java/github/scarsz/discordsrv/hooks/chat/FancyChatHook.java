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

import br.com.finalcraft.fancychat.api.FancyChatApi;
import br.com.finalcraft.fancychat.api.FancyChatSendChannelMessageEvent;
import br.com.finalcraft.fancychat.config.fancychat.FancyChannel;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreBroadcastEvent;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.dv8tion.jda.api.entities.User;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

public class FancyChatHook implements ChatHook {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(FancyChatSendChannelMessageEvent event) {
        // make sure chat channel is registered with a destination
        if (event.getChannel() == null) return;

        // make sure message isn't just blank
        if (StringUtils.isBlank(event.getMessage())) return;

        Player sender = null;
        if (event.getSender() instanceof Player) sender = (Player) event.getSender();

        DiscordSRV.getPlugin().processChatMessage(sender, event.getMessage(), event.getChannel().getName(), false, event);
    }

    @Override
    public void broadcastMessageToChannel(String channel, Component message, User author) {
        FancyChannel fancyChannel = FancyChatApi.getChannel(channel);
        if (fancyChannel == null) return; // no suitable channel found

        DiscordGuildMessagePreBroadcastEvent event = DiscordSRV.api.callEvent(new DiscordGuildMessagePreBroadcastEvent
                (channel, message, author, Collections.unmodifiableList(fancyChannel.getPlayersOnThisChannel())));
        message = event.getMessage();
        if (!channel.equals(event.getChannel())) {
            fancyChannel = FancyChatApi.getChannel(event.getChannel());
            if (fancyChannel == null) return; // no suitable channel found
        }

        String legacy = MessageUtil.toLegacy(message);

        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", "")
                .replace("%channelname%", fancyChannel.getName())
                .replace("%channelnickname%", fancyChannel.getAlias())
                .replace("%message%", legacy);

        String translatedMessage = MessageUtil.translateLegacy(plainMessage);
        FancyChatApi.sendMessage(translatedMessage, fancyChannel);

        List<Player> recipients = fancyChannel.getPlayersOnThisChannel();
        PlayerUtil.notifyPlayersOfMentions(recipients::contains, legacy);
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("EverNifeFancyChat");
    }

}
