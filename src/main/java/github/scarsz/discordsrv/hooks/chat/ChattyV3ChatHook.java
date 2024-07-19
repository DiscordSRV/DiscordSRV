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
import net.kyori.adventure.text.Component;
import github.scarsz.discordsrv.hooks.chat.ChatHook;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import java.util.Collection;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import ru.brikster.chatty.api.ChattyApi;
import ru.brikster.chatty.api.chat.Chat;
import ru.brikster.chatty.api.event.ChattyMessageEvent;

public class ChattyV3ChatHook implements ChatHook {

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Chatty");
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onChattyMessage(ChattyMessageEvent event) {
        DiscordSRV.getPlugin().processChatMessage(event.getSender(), event.getPlainMessage(), event.getChat().getId(), false, (Event)event);
    }

    @Override
    public void broadcastMessageToChannel(String channel, Component message) {
        ChattyApi api = ChattyApi.instance();
        Chat chat = (Chat)api.getChats().get(channel);
        if (chat == null) {
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Attempted to broadcast message to channel \"" + channel + "\" but the channel doesn't exist (returned null); aborting message send");
            return;
        }
        String legacy = MessageUtil.toLegacy(message);
        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString().replace("%channelcolor%", "").replace("%channelname%", chat.getId()).replace("%channelnickname%", chat.getId()).replace("%message%", legacy);
        Collection recipients = chat.calculateRecipients(null);
        DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Sending a message to Chatty chat (" + chat.getId() + "), recipients: " + recipients);
        String translatedMessage = MessageUtil.translateLegacy(plainMessage);
        chat.sendLegacyMessage((Plugin)DiscordSRV.getPlugin(), translatedMessage);
        PlayerUtil.notifyPlayersOfMentions(recipients::contains, legacy);
    }

    @Override
    public boolean isEnabled() {
        boolean regular;
        boolean bl = regular = this.getPlugin() != null && this.getPlugin().isEnabled() && PluginUtil.pluginHookIsEnabled(this.getPlugin().getName());
        if (!regular) {
            return false;
        }
        try {
            Class.forName("ru.brikster.chatty.api.ChattyApi");
        }
        catch (ClassNotFoundException ignore) {
            return false;
        }
        return true;
    }
}
