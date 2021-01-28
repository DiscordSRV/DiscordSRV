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

package github.scarsz.discordsrv.util;

import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import github.scarsz.discordsrv.DiscordSRV;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for sending & editing messages from/for CommandSenders.
 * Utilizes both MiniMessage and Minecraft's legacy formatting style.
 */
public class MessageUtil {

    public static final Pattern DEFAULT_URL_PATTERN = Pattern.compile("(?:(https?)://)?([-\\w_.]+\\.\\w{2,})(/\\S*)?");
    public static final Pattern MINIMESSAGE_PATTERN = Pattern.compile("(?!<@)((?<start><)(?<token>[^<>]+(:(?<inner>['\"]?([^'\"](\\\\['\"])?)+['\"]?))*)(?<end>>))+?");
    public static final Character LEGACY_SECTION = LegacyComponentSerializer.SECTION_CHAR;
    public static final Pattern MESSAGE_PLACEHOLDER = Pattern.compile("%message%.*");
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .useUnusualXRepeatedCharacterHexFormat()
            .extractUrls().hexColors().useUnusualXRepeatedCharacterHexFormat().build();
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
        return plainMessage.indexOf(LEGACY_SECTION) != -1;
    }

    /**
     * Converts the message to a {@link Component} using legacy or MiniMessage format.
     *
     * @param message the message to convert
     * @return the converted {@link Component}
     */
    public static Component toComponent(String message) {
        return toComponent(message, isLegacy(message));
    }

    /**
     * Converts the message to a {@link Component} using legacy or MiniMessage format.
     *
     * @param message the message to convert
     * @param useLegacy if legacy formatting should be used (otherwise MiniMessage)
     * @return the converted {@link Component}
     */
    public static Component toComponent(String message, boolean useLegacy) {
        if (useLegacy) {
            TextComponent component = LEGACY_SERIALIZER.deserialize(message);
            List<Component> children = new ArrayList<>(component.children());
            children.add(0, Component.text(component.content()).style(component.style()));
            component = component.content("").style(Style.empty()).children(children);
            return component;
        } else {
            Component component = MiniMessage.get().parse(message);
            component = component.replaceText(
                    TextReplacementConfig.builder()
                            .match(DEFAULT_URL_PATTERN)
                            .replacement((url) -> (url).clickEvent(ClickEvent.openUrl(url.content())))
                            .build()
            );
            return component;
        }
    }

    /**
     * Converts a given Discord markdown formatted message into a {@link Component} for Minecraft clients.
     *
     * @param discordMessage the Discord markdown formatted message
     * @return the Minecraft {@link Component}
     * @see MinecraftSerializer
     */
    public static Component reserializeToMinecraft(String discordMessage) {
        return MinecraftSerializer.INSTANCE.serialize(discordMessage);
    }

    /**
     * Converts the given legacy or MiniMessage into a Discord formatted string utilizing reserialization.
     *
     * @param component the Minecraft {@link Component}
     * @return a Discord markdown formatted message
     */
    public static String reserializeToDiscord(Component component) {
        return DiscordSerializer.INSTANCE.serialize(component);
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
        return LEGACY_SERIALIZER.serialize(component);
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
        StringBuilder sb = new StringBuilder();
        Matcher matcher = MINIMESSAGE_PATTERN.matcher(plainMessage);

        int lastEnd;
        String start;
        String token;
        String end;
        for (lastEnd = 0; matcher.find(); sb.append("\\").append(start).append(token).append(end)) {
            int startIndex = matcher.start();
            int endIndex = matcher.end();
            if (startIndex > lastEnd) {
                sb.append(plainMessage, lastEnd, startIndex);
            }

            lastEnd = endIndex;
            start = matcher.group("start");
            token = matcher.group("token");
            String inner = matcher.group("inner");
            end = matcher.group("end");
            if (inner != null) {
                token = token.replace(inner, escapeMiniTokens(inner));
            }
        }

        if (plainMessage.length() > lastEnd) {
            sb.append(plainMessage.substring(lastEnd));
        }

        return sb.toString();
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
        commandSenders.forEach(sender -> audiences.add(BUKKIT_AUDIENCES.sender(sender)));
        try {
            Audience.audience(audiences).sendMessage(Identity.nil(), adventureMessage);
        } catch (NoClassDefFoundError e) {
            // might happen with 1.7
            if (e.getMessage().equals("org/bukkit/command/ProxiedCommandSender")) {
                String legacy = toLegacy(adventureMessage);
                commandSenders.forEach(sender -> sender.sendMessage(legacy));
                DiscordSRV.debug(e);
                return;
            }
            DiscordSRV.error(e);
        } catch (Throwable t) {
            DiscordSRV.error(t);
        }
    }

    /**
     * Strips the given String of legacy Minecraft coloring (both & and §) and mini tokens.
     *
     * @param text the given String to strip colors and formatting from
     * @return the given String with coloring and formatting stripped
     * @see #stripLegacy(String)
     * @see #stripMiniTokens(String)
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
        StringBuilder sb = new StringBuilder();
        Matcher matcher = MINIMESSAGE_PATTERN.matcher(text);

        int lastEnd;
        int endIndex;
        for (lastEnd = 0; matcher.find(); lastEnd = endIndex) {
            int startIndex = matcher.start();
            endIndex = matcher.end();
            if (startIndex > lastEnd) {
                sb.append(text, lastEnd, startIndex);
            }
        }

        if (text.length() > lastEnd) {
            sb.append(text.substring(lastEnd));
        }

        return sb.toString();
    }

    /**
     * regex-powered stripping pattern, see https://regex101.com/r/IzirAR/2 for explanation
     */
    private static final Pattern stripPattern = Pattern.compile("(?<!<@)[&§](?i)[0-9a-fklmnorx]");

    /**
     * Strip the given String of legacy Minecraft coloring (both & and §). Useful for sending things to Discord.
     *
     * @param text the given String to strip colors from
     * @return the given String with coloring stripped
     * @see #stripSectionOnlyPattern
     */
    public static String stripLegacy(String text) {
        if (StringUtils.isBlank(text)) {
            DiscordSRV.debug("Tried stripping blank message");
            return "";
        }

        return stripPattern.matcher(text).replaceAll("");
    }

    private static final Pattern stripSectionOnlyPattern = Pattern.compile("(?<!<@)§(?i)[0-9a-fklmnorx]");

    /**
     * Strip the given String of legacy Minecraft coloring (§ only). Useful for sending things to Discord.
     *
     * @param text the given String to strip colors from
     * @return the given String with coloring stripped
     */
    public static String stripLegacySectionOnly(String text) {
        return stripSectionOnlyPattern.matcher(text).replaceAll("");
    }

    private static final Pattern translatePattern = Pattern.compile("(?<!<@)(&)(?i)[0-9a-fklmnorx]");

    /**
     * Translates ampersand (&) characters into section signs (§) for color codes. Ignores role mentions.
     *
     * @param text the input text
     * @return the output text
     */
    public static String translateLegacy(String text) {
        if (text == null) return null;
        Matcher matcher = translatePattern.matcher(text);

        StringBuilder stringBuilder = new StringBuilder(text);
        while (matcher.find()) stringBuilder.setCharAt(matcher.start(1), LEGACY_SECTION);
        return stringBuilder.toString();
    }
}
