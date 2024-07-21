/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import lombok.Getter;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class FeedbackForwardingSenderUtil {

    private final DiscordSRV plugin = DiscordSRV.getPlugin();
    @Getter
    private final CommandSender feedbackSender;
    private final GuildMessageReceivedEvent event;
    private final DiscordSendUtil sendUtil;

    public FeedbackForwardingSenderUtil(GuildMessageReceivedEvent event) {
        feedbackSender = createCommandSender();
        this.event = event;
        this.sendUtil = new DiscordSendUtil(event);
    }

    private CommandSender createCommandSender() {
        try {
            Class<?> serverClass = plugin.getServer().getClass();
            Method createCommandSenderMethod = serverClass.getMethod("createCommandSender", Consumer.class);
            Consumer<Object> serializerFunction = this::processComponent;
            return (CommandSender) createCommandSenderMethod.invoke(plugin.getServer(), serializerFunction);
        } catch (Throwable e) {
            DiscordSRV.error("Error creating commandSender", e);
            return null;
        }
    }

    private void processComponent(Object component) {
        try {
            Class<?> serializerInterface = Class.forName(dot("net{}kyori{}adventure{}text{}serializer{}plain{}PlainTextComponentSerializer"));
            Class<?> componentInterface = Class.forName(dot("net{}kyori{}adventure{}text{}Component"));

            Method plainTextMethod = serializerInterface.getMethod("plainText");
            Method serializeMethod = serializerInterface.getMethod("serialize", componentInterface);

            Object serializerInstance = plainTextMethod.invoke(null);

            sendUtil.send((String) serializeMethod.invoke(serializerInstance, component));
        } catch (Throwable e) {
            DiscordSRV.error("Error serializing non-relocated component to String", e);
        }
    }

    private String dot(String str) {
        return str.replace("{}", "."); // Used for relocation bypass
    }

    public static boolean senderExists() {
        try {
            Class.forName("io.papermc.paper.commands.FeedbackForwardingSender");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
