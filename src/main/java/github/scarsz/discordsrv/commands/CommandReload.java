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

package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.ConfigReloadedEvent;
import github.scarsz.discordsrv.util.LangUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class CommandReload {

    @Command(commandNames = { "reload" },
            helpMessage = "Reloads the config of DiscordSRV",
            permission = "discordsrv.reload"
    )
    public static void execute(CommandSender sender, String[] args) {
        DiscordSRV.getPlugin().reloadConfig();
        DiscordSRV.getPlugin().reloadCancellationDetector();
        DiscordSRV.getPlugin().reloadChannels();
        DiscordSRV.getPlugin().reloadColors();

        sender.sendMessage(ChatColor.AQUA + LangUtil.InternalMessage.RELOADED.toString());

        DiscordSRV.api.callEvent(new ConfigReloadedEvent(sender));
    }

}
