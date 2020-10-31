/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
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

import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import ru.mrbrikster.chatty.api.ChattyApi;
import ru.mrbrikster.chatty.api.chats.Chat;
import ru.mrbrikster.chatty.api.events.ChattyMessageEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class ChattyChatHook implements ChatHook {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChattyMessage(ChattyMessageEvent event) {
        DiscordSRV.getPlugin().processChatMessage(event.getPlayer(), event.getMessage(), event.getChat().getName(), false);
    }

    @Override
    public void broadcastMessageToChannel(String channel, String message) {
        ChattyApi api = getApi();
        if (api == null) return;

        Optional<Chat> optChat = api.getChat(channel);
        if (!optChat.isPresent()) {
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Attempted to broadcast message to channel \"" + channel + "\" but the channel doesn't exist (returned null); aborting message send");
            return;
        }

        Chat chat = optChat.get();
        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", "")
                .replace("%channelname%", chat.getName())
                .replace("%channelnickname%", chat.getName())
                .replace("%message%", message);

        if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft")) {
            chat.sendMessage(LegacyComponentSerializer.INSTANCE.serialize(MinecraftSerializer.INSTANCE.serialize(plainMessage)));
        } else {
            chat.sendMessage(ChatColor.translateAlternateColorCodes('&', plainMessage));
        }
    }

    private ChattyApi getApi() {
        Plugin chatty = getPlugin();
        try {
            return (ChattyApi) chatty.getClass().getMethod("api").invoke(chatty);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            DiscordSRV.error("Unable to get Chatty plugin");
            e.printStackTrace();
            return null;
        }
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
            Class.forName("ru.mrbrikster.chatty.api.ChattyApi");
        } catch (ClassNotFoundException ignore) {
            return false;
        }
        return true;
    }
}
