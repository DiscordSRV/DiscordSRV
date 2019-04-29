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

import github.scarsz.discordsrv.util.DebugUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

public class CommandDebug {

    @Command(commandNames = { "debug" },
            helpMessage = "Dumps DiscordSRV debug information to GitHub Gists or the plugin folder",
            permission = "discordsrv.debug"
    )
    public static void execute(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.AQUA + "Your debug report has been generated and is available at " + DebugUtil.run(sender instanceof ConsoleCommandSender ? "CONSOLE" : sender.getName()) + ".");
    }

}
