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

package github.scarsz.discordsrv.hooks;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapWebChatEvent;

public class DynmapHook implements PluginHook {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDynmapWebChat(DynmapWebChatEvent event) {
        String format = LangUtil.Message.DYNMAP_DISCORD_FORMAT.toString()
                .replace("%message%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(event.getMessage())))
                .replace("%name%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(event.getName())));

        if (!DiscordSRV.config().getBoolean("DiscordChatChannelTranslateMentions")) {
            format = format.replace("@", "@\u200B"); // zero-width space
        }

        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), format);
    }

    public void broadcastMessageToDynmap(String name, String message) {
        try {
            DynmapCommonAPI api = (DynmapCommonAPI) getPlugin();
            if (api == null) return;
            api.sendBroadcastToWeb(name, message);
        } catch (Throwable t) {
            DiscordSRV.warning("Failed to send message to dynmap: " + t.toString());
            DiscordSRV.debug(ExceptionUtils.getStackTrace(t));
        }
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("dynmap");
    }
}
