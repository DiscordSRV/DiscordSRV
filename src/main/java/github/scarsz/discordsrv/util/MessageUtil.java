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

package github.scarsz.discordsrv.util;

import me.minidigger.minimessage.text.MiniMessageParser;
import me.minidigger.minimessage.text.MiniMessageSerializer;
import net.kyori.text.Component;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

import java.util.Collections;

public class MessageUtil {

    private MessageUtil() {}

    public static boolean isLegacy(String plainMessage) {
        return plainMessage.indexOf(LegacyComponentSerializer.CHARACTER) > 0;
    }

    public static Component toComponent(String plainMessage) {
        return isLegacy(plainMessage)
                ? LegacyComponentSerializer.legacy().deserialize(plainMessage)
                : MiniMessageParser.parseFormat(plainMessage);
    }

    public static String toMiniMessage(Component component) {
        return MiniMessageSerializer.serialize(component);
    }

    public static String toLegacy(Component component) {
        return LegacyComponentSerializer.legacy().serialize(component);
    }

    public static String toPlain(Component component, boolean isLegacy) {
        return isLegacy ? toLegacy(component) : toMiniMessage(component);
    }

    public static String escapeMiniTokens(String miniMessage) {
        return MiniMessageParser.escapeTokens(miniMessage);
    }

    /**
     * Translates the plain message from legacy section sign format or MiniMessage format to a {@link Component} and sends it to the provided {@link CommandSender}.
     *
     * @param commandSender the command sender to send the component to
     * @param plainMessage the legacy or section sign format or MiniMessage formatted message
     */
    public static void sendMessage(CommandSender commandSender, String plainMessage) {
        sendMessage(Collections.singleton(commandSender), plainMessage);
    }

    /**
     * Sends the provided {@link Component} to the provided {@link CommandSender}.
     *
     * @param commandSender the command sender to send the component to
     * @param adventureMessage the message to send
     */
    public static void sendMessage(CommandSender commandSender, Component adventureMessage) {
        sendMessage(Collections.singleton(commandSender), adventureMessage);
    }

    /**
     * Translates the plain message from legacy section sign format or MiniMessage format to a {@link Component} and sends it to the provided {@link CommandSender}s.
     *
     * @param commandSenders the command senders to send the component to
     * @param plainMessage the legacy or section sign format or MiniMessage formatted message
     */
    public static void sendMessage(Iterable<? extends CommandSender> commandSenders, String plainMessage) {
        sendMessage(commandSenders, toComponent(plainMessage));
    }

    /**
     * Sends the provided {@link Component} to the provided {@link CommandSender}s.
     *
     * @param commandSenders the command senders to send the component to
     * @param adventureMessage the message to send
     */
    public static void sendMessage(Iterable<? extends CommandSender> commandSenders, Component adventureMessage) {
        TextAdapter.sendMessage(commandSenders, adventureMessage);
    }
}
