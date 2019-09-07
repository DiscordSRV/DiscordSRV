/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
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
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    public PlayerDeathListener() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        if (StringUtils.isBlank(event.getDeathMessage())) return;
        if (StringUtils.isBlank(LangUtil.Message.PLAYER_DEATH.toString())) return;

        // respect invisibility plugins
        if (PlayerUtil.isVanished(event.getEntity())) return;

        String discordMessage = LangUtil.Message.PLAYER_DEATH.toString()
                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%username%", event.getEntity().getName())
                .replace("%displayname%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(event.getEntity().getDisplayName())))
                .replace("%world%", event.getEntity().getWorld().getName())
                .replace("%deathmessage%", DiscordUtil.strip(DiscordUtil.escapeMarkdown(event.getDeathMessage())));
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) discordMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(event.getEntity(), discordMessage);

        discordMessage = DiscordUtil.strip(discordMessage);
        if (StringUtils.isBlank(discordMessage)) return;
        String legnthCheckMessage = discordMessage.replaceAll("[^A-z]", "");
        if (StringUtils.isBlank(legnthCheckMessage)) return;

        if (legnthCheckMessage.length() < 3) {
            DiscordSRV.debug("Not sending death message \"" + discordMessage + "\" because it's less than three characters long");
            return;
        }

        DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), discordMessage);
    }

}
