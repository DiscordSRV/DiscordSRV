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

package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.*;

public class CommandDebugger {

    @Command(commandNames = { "debugger" },
            helpMessage = "A toggleable timings-like command to dump debug information to bin.scarsz.me",
            permission = "discordsrv.debug"
    )
    public static void execute(CommandSender sender, String[] args) {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        String subCommand;
        if (arguments.isEmpty()) {
            subCommand = "start";
        } else {
            subCommand = arguments.remove(0);
        }

        boolean upload = false;
        if (subCommand.equalsIgnoreCase("start") || subCommand.equalsIgnoreCase("on")) {
            Set<String> validArguments = new HashSet<>();
            for (String argument : arguments) {
                boolean anyValid = false;
                for (Debug value : Debug.values()) {
                    if (value.matches(argument)) {
                        anyValid = true;
                        break;
                    }
                }
                if (!anyValid) {
                    sender.sendMessage(ChatColor.RED + "Invalid debug category: " + ChatColor.DARK_RED + argument);
                    continue;
                }

                validArguments.add(argument);
            }

            if (validArguments.isEmpty()) {
                DiscordSRV.getPlugin().getDebuggerCategories().add(Debug.UNCATEGORIZED.name());
            } else {
                DiscordSRV.getPlugin().getDebuggerCategories().addAll(validArguments);
            }
            sender.sendMessage(ChatColor.DARK_AQUA + "Debugger enabled, use "
                    + ChatColor.GRAY + "/discordsrv debugger stop " + ChatColor.DARK_AQUA + "to stop debugging or "
                    + ChatColor.GRAY + "/discordsrv debugger upload " + ChatColor.DARK_AQUA + "to stop debugging and generate a debug report");
            return;
        } else if (subCommand.equalsIgnoreCase("stop") || subCommand.equalsIgnoreCase("off")
                || (upload = subCommand.equalsIgnoreCase("upload"))) {
            DiscordSRV.getPlugin().getDebuggerCategories().clear();

            if (upload) {
                CommandDebug.execute(sender, arguments.toArray(new String[0]));
            } else {
                sender.sendMessage(ChatColor.DARK_AQUA + "Debugger disabled");
            }
            return;
        }

        sender.sendMessage(ChatColor.RED + "Invalid subcommand " + ChatColor.DARK_RED + subCommand);
    }
}
