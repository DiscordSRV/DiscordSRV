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

package github.scarsz.discordsrv.objects.managers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.commands.*;
import github.scarsz.discordsrv.util.GamePermissionUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager {

    @Getter private Map<String, Method> commands = new HashMap<>();

    public CommandManager() {
        List<Class<?>> commandClasses = Arrays.asList(
                CommandBroadcast.class,
                CommandDebug.class,
                CommandHelp.class,
                CommandLanguage.class,
                CommandLink.class,
                CommandLinked.class,
                CommandReload.class,
                CommandResync.class,
                CommandUnlink.class
        );

        for (Class<?> clazz : commandClasses) {
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(Command.class)) continue; // make sure method is marked as a command

                if (method.getParameters().length != 2) {
                    DiscordSRV.debug("Method " + method.toGenericString().replace("public static void ", "") + " annotated as command but parameters count != 2");
                    continue;
                }
                if (method.getParameters()[0].getType() != CommandSender.class && method.getParameters()[0].getType() != Player.class) {
                    DiscordSRV.debug("Method " + method.toGenericString().replace("public static void ", "") + " annotated as command but parameter 1's type != CommandSender || Player");
                    continue;
                }
                if (method.getParameters()[1].getType() != String[].class) {
                    DiscordSRV.debug("Method " + method.toGenericString().replace("public static void ", "") + " annotated as command but parameter 2's type != String[]");
                    continue;
                }

                Command annotation = method.getAnnotation(Command.class);
                for (String commandName : annotation.commandNames()) commands.put(commandName.toLowerCase(), method);
            }
        }
    }

    public boolean handle(CommandSender sender, String command, String[] args) {
        if (command == null) {
            String message = LangUtil.Message.DISCORD_COMMAND.toString()
                    .replace("{INVITE}", DiscordSRV.config().getString("DiscordInviteLink"));
            for (String line : message.split("\n")) MessageUtil.sendMessage(sender, line);
            return true;
        }

        if (commands.containsKey(command.toLowerCase())) {
            try {
                Method commandMethod = commands.get(command.toLowerCase());
                Command commandAnnotation = commandMethod.getAnnotation(Command.class);

                if (!GamePermissionUtil.hasPermission(sender, commandAnnotation.permission())) {
                    MessageUtil.sendMessage(sender, LangUtil.Message.NO_PERMISSION.toString());
                    return true;
                }

                if (commandMethod.getParameters()[0].getType() == Player.class && !(sender instanceof Player)) {
                    MessageUtil.sendMessage(sender, ChatColor.RED + LangUtil.InternalMessage.PLAYER_ONLY_COMMAND.toString());
                    return true;
                }

                commandMethod.invoke(null, sender, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                MessageUtil.sendMessage(sender, ChatColor.RED + "" + LangUtil.InternalMessage.COMMAND_EXCEPTION);
                DiscordSRV.error(e);
            }
        } else {
            MessageUtil.sendMessage(sender, LangUtil.Message.COMMAND_DOESNT_EXIST.toString());
        }

        return true;
    }

}
