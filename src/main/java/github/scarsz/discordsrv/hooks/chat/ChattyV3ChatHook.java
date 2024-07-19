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

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import ru.brikster.chatty.api.ChattyApi;
import ru.brikster.chatty.api.chat.Chat;
import ru.brikster.chatty.api.event.ChattyMessageEvent;

import java.util.Collection;
import java.util.Optional;

public class ChattyV3ChatHook implements ChatHook {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChattyMessage(ChattyMessageEvent event) {
        DiscordSRV.getPlugin().processChatMessage(event.getSender().getPlayer(), event.getPlainMessage(), event.getChat().getId(), false, event);
    }

    @Override
    public void broadcastMessageToChannel(String channel, Component message) {
        ChattyApi api = getApi();

        Optional<Chat> optChat = Optional.ofNullable(api.getChats().get(channel));
        if (!optChat.isPresent()) {
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Attempted to broadcast message to channel \"" + channel + "\" but the channel doesn't exist (returned null); aborting message send");
            return;
        }

        Chat chat = optChat.get();
        String legacy = MessageUtil.toLegacy(message);
        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", "")
                .replace("%channelname%", chat.getId())
                .replace("%channelnickname%", chat.getId())
                .replace("%message%", legacy);

        Collection<? extends Player> recipients = DiscordSRV.getPlugin().getServer().getOnlinePlayers();
        DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Sending a message to Chatty chat (" + chat.getId() + "), recipients: " + recipients);

        String translatedMessage = MessageUtil.translateLegacy(plainMessage);
        chat.sendLegacyMessage(DiscordSRV.getPlugin(), translatedMessage);
        PlayerUtil.notifyPlayersOfMentions(recipients::contains, legacy);
    }

    private ChattyApi getApi() {
        return ChattyApi.instance();
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Chatty");
    }

    @Override
    public boolean isEnabled() {
        boolean regular = getPlugin() != null && getPlugin().isEnabled() && PluginUtil.pluginHookIsEnabled(getPlugin().getName());
        if (!regular) return false;

        try {
            Class.forName("ru.brikster.chatty.api.ChattyApi");
        } catch (ClassNotFoundException ignore) {
            return false;
        }
        return true;
    }

}
