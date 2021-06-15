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

/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2021 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;

public class ModernPlayerChatListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!DiscordSRV.config().getBooleanElse("UseModernPaperChatEvent", false)) return;

        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            String json;
            try {
                // workaround for us having a relocated version of adventure
                // methods are grabbed from interfaces due to implementations being inaccessible
                Class<?> gsonClass = Class.forName("net.ky".concat("ori.adventure.text.serializer.gson.GsonComponentSerializer"));
                Class<?> componentClass = Class.forName("net.ky".concat("ori.adventure.text.Component"));

                Method message = event.getClass().getMethod("message");
                Object eventMessage = message.invoke(event);

                Method gson = gsonClass.getMethod("gson");
                Object gsonSerializer = gson.invoke(null);

                Method serialize = gsonClass.getMethod("serialize", componentClass);
                json = (String) serialize.invoke(gsonSerializer, eventMessage);
            } catch (Throwable t) {
                DiscordSRV.error("Unable to get JSON from Paper Component", t);
                return;
            }

            DiscordSRV.getPlugin().processChatMessage(
                    event.getPlayer(),
                    GsonComponentSerializer.gson().deserialize(json),
                    DiscordSRV.getPlugin().getOptionalChannel("global"),
                    event.isCancelled()
            );
        });
    }
}
