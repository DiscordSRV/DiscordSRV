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

import github.scarsz.configuralize.DynamicConfig;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.MessageFormat;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class MessageFormatResolver {

    public static String getMessageFormat(List<Role> selectedRoles, String channel) {
        LangUtil.Message format = !selectedRoles.isEmpty() ? LangUtil.Message.CHAT_TO_MINECRAFT : LangUtil.Message.CHAT_TO_MINECRAFT_NO_ROLE;

        return DiscordSRV.config().getOptionalString(format.getKeyName() + "_" + channel)
                .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                .orElseGet(format::toString);
    }

    public static MessageFormat getMessageFromConfiguration(DynamicConfig config, String key) {
        if (!config.getOptional(key).isPresent()) {
            return null;
        }

        Optional<Boolean> enabled = config.getOptionalBoolean(key + ".Enabled");
        if (enabled.isPresent() && !enabled.get()) {
            return null;
        }

        MessageFormat messageFormat = new MessageFormat();

        if (config.getOptional(key + ".Embed").isPresent() && config.getOptionalBoolean(key + ".Embed.Enabled").orElse(true)) {
            Optional<String> hexColor = config.getOptionalString(key + ".Embed.Color");
            if (hexColor.isPresent()) {
                String hex = hexColor.get().trim();
                if (!hex.startsWith("#")) hex = "#" + hex;
                if (hex.length() == 7) {
                    messageFormat.setColor(
                            new Color(
                                    Integer.valueOf(hex.substring(1, 3), 16),
                                    Integer.valueOf(hex.substring(3, 5), 16),
                                    Integer.valueOf(hex.substring(5, 7), 16)
                            )
                    );
                } else {
                    DiscordSRV.debug("Invalid color hex: " + hex + " (in " + key + ".Embed.Color)");
                }
            } else {
                config.getOptionalInt(key + ".Embed.Color").map(Color::new).ifPresent(messageFormat::setColor);
            }

            if (config.getOptional(key + ".Embed.Author").isPresent()) {
                config.getOptionalString(key + ".Embed.Author.Name")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setAuthorName);
                config.getOptionalString(key + ".Embed.Author.Url")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setAuthorUrl);
                config.getOptionalString(key + ".Embed.Author.ImageUrl")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setAuthorImageUrl);
            }

            config.getOptionalString(key + ".Embed.ThumbnailUrl")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setThumbnailUrl);

            config.getOptionalString(key + ".Embed.Title.Text")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setTitle);

            config.getOptionalString(key + ".Embed.Title.Url")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setTitleUrl);

            config.getOptionalString(key + ".Embed.Description")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setDescription);

            Optional<List<String>> fieldsOptional = config.getOptionalStringList(key + ".Embed.Fields");
            if (fieldsOptional.isPresent()) {
                List<MessageEmbed.Field> fields = new ArrayList<>();
                for (String s : fieldsOptional.get()) {
                    if (s.contains(";")) {
                        String[] parts = s.split(";");
                        if (parts.length < 2) {
                            continue;
                        }

                        boolean inline = parts.length < 3 || Boolean.parseBoolean(parts[2]);
                        fields.add(new MessageEmbed.Field(parts[0], parts[1], inline, true));
                    } else {
                        boolean inline = Boolean.parseBoolean(s);
                        fields.add(new MessageEmbed.Field("\u200e", "\u200e", inline, true));
                    }
                }
                messageFormat.setFields(fields);
            }

            config.getOptionalString(key + ".Embed.ImageUrl")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setImageUrl);

            if (config.getOptional(key + ".Embed.Footer").isPresent()) {
                config.getOptionalString(key + ".Embed.Footer.Text")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setFooterText);
                config.getOptionalString(key + ".Embed.Footer.IconUrl")
                        .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setFooterIconUrl);
            }

            Optional<Boolean> timestampOptional = config.getOptionalBoolean(key + ".Embed.Timestamp");
            if (timestampOptional.isPresent()) {
                if (timestampOptional.get()) {
                    messageFormat.setTimestamp(new Date().toInstant());
                }
            } else {
                Optional<Long> epochOptional = config.getOptionalLong(key + ".Embed.Timestamp");
                epochOptional.ifPresent(timestamp -> messageFormat.setTimestamp(new Date(timestamp).toInstant()));
            }
        }

        if (config.getOptional(key + ".Webhook").isPresent() && config.getOptionalBoolean(key + ".Webhook.Enable").orElse(false)) {
            messageFormat.setUseWebhooks(true);
            config.getOptionalString(key + ".Webhook.AvatarUrl")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setWebhookAvatarUrl);
            config.getOptionalString(key + ".Webhook.Name")
                    .filter(StringUtils::isNotBlank).ifPresent(messageFormat::setWebhookName);
        }

        Optional<String> content = config.getOptionalString(key + ".Content");
        if (content.isPresent() && StringUtils.isNotBlank(content.get())) {
            messageFormat.setContent(content.get());
        }

        return messageFormat.isAnyContent() ? messageFormat : null;
    }
}
