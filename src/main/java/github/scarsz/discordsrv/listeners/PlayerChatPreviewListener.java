/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.hooks.chat.ChatHook;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class PlayerChatPreviewListener {

    @SuppressWarnings("deprecation")
    public PlayerChatPreviewListener() {
        try {
            Class<?> eventClass = Class.forName("org.bukkit.event.player.AsyncPlayerChatPreviewEvent");

            // AsyncPlayerChatPreviewEvent is still considered a "draft API" and may receive future changes that
            // break backward compatibility, so we should check to make sure it still extends AsyncPlayerChatEvent
            if (!AsyncPlayerChatEvent.class.isAssignableFrom(eventClass)) {
                DiscordSRV.error("AsyncPlayerChatPreviewEvent doesn't extend AsyncPlayerChatEvent, "
                        + "likely due to a newer server version; typing indicators will not function");
                return;
            }

            RegisteredListener registeredListener = new RegisteredListener(
                    new Listener() {},
                    (listener, event) -> onAsyncPlayerChatPreview((AsyncPlayerChatEvent) event),
                    EventPriority.MONITOR,
                    DiscordSRV.getPlugin(),
                    false
            );

            HandlerList handlerList = (HandlerList) eventClass.getMethod("getHandlerList").invoke(null);
            handlerList.register(registeredListener);
        } catch (ClassNotFoundException e) {
            // The server version is <1.19.1
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            DiscordSRV.error("Failed to get the handler list for AsyncPlayerChatPreviewEvent, "
                    + "typing indicators will not function");
        }
    }

    @SuppressWarnings("deprecation")
    private void onAsyncPlayerChatPreview(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Component message = MessageUtil.toComponent(event.getMessage(), true);
        if (DiscordSRV.getPlugin().skipProcessingChatMessage(player, message, event.isCancelled(), false)) {
            return;
        }

        Optional<ChatHook> chatHook = DiscordSRV.getPlugin()
                .getPluginHooks()
                .stream()
                .filter(hook -> hook instanceof ChatHook)
                .map(hook -> (ChatHook) hook)
                .findAny();

        TextChannel channel;
        if (chatHook.isPresent()) {
            String channelName = chatHook.get().getPrimaryChannelOfPlayer(player);
            if (channelName == null) {
                return;
            }
            channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
        } else {
            channel = DiscordSRV.getPlugin().getOptionalTextChannel("global");
        }

        if (channel != null) {
            DiscordUtil.updateTypingIndicator(channel);
        }
    }

}
