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

package github.scarsz.discordsrv.hooks.chat;

import br.net.fabiozumbi12.UltimateChat.Bukkit.API.SendChannelMessageEvent;
import br.net.fabiozumbi12.UltimateChat.Bukkit.UCChannel;
import br.net.fabiozumbi12.UltimateChat.Bukkit.UChat;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;

public class UltimateChatHook implements ChatHook {

    private Class<?> ucJsonArrayClass;
    private Class<?> ucJsonValueClass;
    private Constructor<?> ultimateFancyConstructor;
    private Method sendMessageMethod;

    public UltimateChatHook() {}

    @SuppressWarnings("UnnecessaryBoxing")
    @Override
    public void hook() {
        Class<?> ultimateFancyClass;
        try {
            ultimateFancyClass = Class.forName("br.net.fabiozumbi12.UltimateFancy.UltimateFancy");
            ucJsonArrayClass = Class.forName("br.net.fabiozumbi12.UltimateFancy.jsonsimple.JSONArray");
            ucJsonValueClass = Class.forName("br.net.fabiozumbi12.UltimateFancy.jsonsimple.JSONValue");
        } catch (ClassNotFoundException ignored) {
            try {
                ultimateFancyClass = Class.forName("br.net.fabiozumbi12.UltimateChat.Bukkit.UltimateFancy");
                ucJsonArrayClass = Class.forName("org.js" + new Character('o') + "n.simple.JSONArray");
                ucJsonValueClass = Class.forName("org.js" + new Character('o') + "n.simple.JSONValue");
            } catch (ClassNotFoundException ignoreThis) {
                try {
                    ultimateFancyClass = Class.forName("br.net.fabiozumbi12.UltimateChat.Bukkit.util.UltimateFancy");
                    ucJsonArrayClass = Class.forName("org.js" + new Character('o') + "n.simple.JSONArray");
                    ucJsonValueClass = Class.forName("org.js" + new Character('o') + "n.simple.JSONValue");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("No UltimateFancy class found to use for UltimateChat hook", e);
                }
            }
        }

        try {
            if (Arrays.stream(ultimateFancyClass.getConstructors())
                    .anyMatch(constructor -> constructor.getParameterCount() == 0)) {
                ultimateFancyConstructor = ultimateFancyClass.getDeclaredConstructor();
            } else {
                ultimateFancyConstructor = ultimateFancyClass.getDeclaredConstructor(JavaPlugin.class);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to find UltimateFancy constructor: " + e.getMessage(), e);
        }

        try {
            sendMessageMethod = Class.forName("br.net.fabiozumbi12.UltimateChat.Bukkit.UCChannel").getMethod("sendMessage", ConsoleCommandSender.class, ultimateFancyClass, boolean.class);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to get sendMessage method of UCChannel in UltimateChat hook", e);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(SendChannelMessageEvent event) {
        // make sure chat channel is registered with a destination
        if (event.getChannel() == null) return;

        // make sure message isn't just blank
        if (StringUtils.isBlank(event.getMessage())) return;

        Player sender = null;
        if (event.getSender() instanceof Player) sender = (Player) event.getSender();

        DiscordSRV.getPlugin().processChatMessage(sender, event.getMessage(), event.getChannel().getName(), false);
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void broadcastMessageToChannel(String channel, Component message) {
        UCChannel chatChannel = getChannelByCaseInsensitiveName(channel);
        if (chatChannel == null) return; // no suitable channel found

        String format = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString();
        Component plainMessage = MessageUtil.toComponent(
                format.replace("%channelcolor%", MessageUtil.toPlain(MessageUtil.toComponent(MessageUtil.translateLegacy(chatChannel.getColor())), MessageUtil.isLegacy(format)))
                        .replace("%channelname%", chatChannel.getName())
                        .replace("%channelnickname%", chatChannel.getAlias())
        );

        plainMessage = plainMessage.replaceText(TextReplacementConfig.builder().match(MessageUtil.MESSAGE_PLACEHOLDER)
                .replacement(builder -> message.append(builder.content(builder.content().replaceFirst("%message%", ""))))
                .build());

        Object ultimateFancy;
        try {
            if (ultimateFancyConstructor.getParameterCount() == 0) {
                // older UltimateFancy version
                ultimateFancy = ultimateFancyConstructor.newInstance();
            } else {
                ultimateFancy = ultimateFancyConstructor.newInstance(DiscordSRV.getPlugin());
            }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            DiscordSRV.error("Failed to initialize UltimateFancy in UltimateChat hook: " + e.getMessage());
            return;
        }

        try {
            // what you might think is a simple task, isn't simple
            // these classes are relocated by UC
            // plus UC's api doesn't handle empty text so we need to give it the already parsed *simple-json* object

            String json = GsonComponentSerializer.gson().serialize(plainMessage);
            Object jsonArray = ucJsonArrayClass.newInstance();

            Method parseMethod = ucJsonValueClass.getDeclaredMethod("parse", String.class);
            Object jsonObject = parseMethod.invoke(null, json);

            ((Collection<Object>) jsonArray).add(jsonObject);

            // despite the name, this is where json is added
            Method setContructorMethod = ultimateFancy.getClass().getDeclaredMethod("setContructor", ucJsonArrayClass);

            setContructorMethod.invoke(ultimateFancy, jsonArray);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            DiscordSRV.error("Failed to add JSON to UltimateChat UltimateFancy class " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return;
        }

        try {
            sendMessageMethod.invoke(chatChannel, Bukkit.getServer().getConsoleSender(), ultimateFancy, true);
        } catch (IllegalAccessException | InvocationTargetException e) {
            DiscordSRV.error("Failed to invoke sendMessage on UCChannel in UltimateChat hook: " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return;
        }

        PlayerUtil.notifyPlayersOfMentions(player -> chatChannel.getMembers().contains(player.getName()), MessageUtil.toPlain(message, true));
    }

    private static UCChannel getChannelByCaseInsensitiveName(String name) {
        for (UCChannel channel : UChat.get().getAPI().getChannels())
            if (channel.getName().equalsIgnoreCase(name)) return channel;
        return null;
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("UltimateChat");
    }

}
