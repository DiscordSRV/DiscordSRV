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

import github.scarsz.discordsrv.hooks.PluginHook;
import github.scarsz.discordsrv.util.MessageUtil;
import net.kyori.adventure.text.Component;

public interface ChatHook extends PluginHook {

    @Deprecated
    default void broadcastMessageToChannel(String channel, String message) {}

    default void broadcastMessageToChannel(String channel, Component message) {
        broadcastMessageToChannel(channel, MessageUtil.toLegacy(message));
    }

}
