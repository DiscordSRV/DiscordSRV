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

import github.scarsz.discordsrv.DiscordSRV;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for sending & editing messages from/for CommandSenders.
 * Utilizes both MiniMessage and Minecraft's legacy formatting style.
 */
public class MessageUtil {

    public static final Character LEGACY_SECTION = LegacyComponentSerializer.SECTION_CHAR;
    private static final BukkitAudiences BUKKIT_AUDIENCES;

    static {
        BUKKIT_AUDIENCES = BukkitAudiences.create(DiscordSRV.getPlugin());
    }

    private MessageUtil() {}

    /**
     * Determines weather or not to use legacy instead of MiniMessage format, by checking if the message contains a section sign (the legacy character).
     *
     * @param plainMessage the message to convert
     * @return true if the message contained a section sign
     */
    public static boolean isLegacy(String plainMessage) {
        return plainMessage.indexOf(LEGACY_SECTION) > 0;
    }

    /**
     * Converts the message to a {@link Component} using legacy or MiniMessage format.
     *
     * @param plainMessage the message to convert
     * @return the converted {@link Component}
     */
    public static Component toComponent(String plainMessage) {
        return isLegacy(plainMessage)
                ? LegacyComponentSerializer.legacySection().deserialize(plainMessage)
                : MiniMessage.get().parse(plainMessage);
    }

    /**
     * Converts a {@link Component} to a MiniMessage.
     *
     * @param component the component to convert
     * @return the converted MiniMessage
     */
    public static String toMiniMessage(Component component) {
        return MiniMessage.get().serialize(component);
    }

    /**
     * Coverts a {@link Component} to a legacy message.
     *
     * @param component the component to convert
     * @return the converted legacy message
     */
    public static String toLegacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    /**
     * Converts the {@link Component} to a legacy or MiniMessage message.
     *
     * @param component the component to convert
     * @param isLegacy weather or not to use legacy or MiniMessage
     * @return the converted legacy or MiniMessage message
     */
    public static String toPlain(Component component, boolean isLegacy) {
        return isLegacy ? toLegacy(component) : toMiniMessage(component);
    }

    /**
     * Escapes MiniMessage tokens, for input sanitization.
     * Useful when sending messages to Discord.
     *
     * @param plainMessage the input message
     * @return the message with mini tokens escaped
     */
    public static String escapeMiniTokens(String plainMessage) {
        return MiniMessage.get().escapeTokens(plainMessage);
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
        Set<Audience> audiences = new HashSet<>();
        commandSenders.forEach(sender -> audiences.add(BUKKIT_AUDIENCES.audience(sender)));
        Audience.of(audiences).sendMessage(adventureMessage);
    }

    /**
     * Strips the given String of legacy Minecraft coloring (both & and §) and mini tokens.
     *
     * @param text the given String to strip colors and formatting from
     * @return the given String with coloring and formatting stripped
     */
    public static String strip(String text) {
        return stripLegacy(stripMiniTokens(text));
    }

    /**
     * Strips the given String of mini tokens.
     *
     * @param text the given String to strip mini tokens from
     * @return the given String with mini tokens stripped
     */
    public static String stripMiniTokens(String text) {
        return MiniMessage.get().stripTokens(text);
    }

    /**
     * regex-powered stripping pattern, see https://regex101.com/r/IzirAR/2 for explanation
     */
    private static final Pattern stripPattern = Pattern.compile("(?<!@)[&§](?i)[0-9a-fklmnorx]");

    /**
     * Strip the given String of legacy Minecraft coloring (both & and §). Useful for sending things to Discord.
     *
     * @param text the given String to strip colors from
     * @return the given String with coloring stripped
     */
    public static String stripLegacy(String text) {
        if (StringUtils.isBlank(text)) {
            DiscordSRV.debug("Tried stripping blank message");
            return "";
        }

//        TODO: revisit this
//        // Replace invisible control characters and unused code points
//        StringBuilder newString = new StringBuilder(newText.length());
//        for (int offset = 0; offset < newText.length();) {
//            if (newText.substring(offset, offset + 1).equals("\n")) {
//                newString.append("\n");
//                continue;
//            }
//
//            int codePoint = newText.codePointAt(offset);
//            offset += Character.charCount(codePoint);
//
//            switch (Character.getType(codePoint)) {
//                case Character.CONTROL:     // \p{Cc}
//                case Character.FORMAT:      // \p{Cf}
//                case Character.PRIVATE_USE: // \p{Co}
//                case Character.SURROGATE:   // \p{Cs}
//                case Character.UNASSIGNED:  // \p{Cn}
//                    break;
//                default:
//                    newString.append(Character.toChars(codePoint));
//                    break;
//            }
//        }
//
//        return newString.toString();

        return stripPattern.matcher(text).replaceAll("");
    }

    private static final Pattern stripSectionOnlyPattern = Pattern.compile("(?<!@)§(?i)[0-9a-fklmnorx]");

    /**
     * Strip the given String of legacy Minecraft coloring (§ only). Useful for sending things to Discord.
     *
     * @param text the given String to strip colors from
     * @return the given String with coloring stripped
     */
    public static String stripLegacySectionOnly(String text) {
        return stripSectionOnlyPattern.matcher(text).replaceAll("");
    }

}
