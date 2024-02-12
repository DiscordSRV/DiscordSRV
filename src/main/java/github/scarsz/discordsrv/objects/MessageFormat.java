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

package github.scarsz.discordsrv.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageFormat {

    // Regular message
    private String content;

    // Embed contents
    private String authorName;
    private String authorUrl;
    private String authorImageUrl;
    private String thumbnailUrl;
    private String title;
    private String titleUrl;
    private String description;
    private String imageUrl;
    private String footerText;
    private String footerIconUrl;
    private Instant timestamp;
    private int colorRaw = -1;
    private List<MessageEmbed.Field> fields;

    // Webhook capabilities
    private boolean useWebhooks;
    private String webhookAvatarUrl;
    private String webhookName;

    public boolean isAnyContent() {
        return content != null || authorName != null || authorUrl != null || authorImageUrl != null
                || thumbnailUrl != null || title != null || titleUrl != null || description != null
                || imageUrl != null || fields != null || footerText != null;
    }

    public int getColorRaw() {
        // Default -> Default
        return colorRaw == -1 ? Role.DEFAULT_COLOR_RAW : colorRaw;
    }

    /**
     * @deprecated Use {@link #getColorRaw()} instead.
     */
    @Deprecated
    public java.awt.Color getColor() {
        return new java.awt.Color(colorRaw);
    }

    /**
     * @deprecated Use {@link #setColorRaw(int)} instead.
     */
    @Deprecated
    public void setColor(java.awt.Color color) {
        this.colorRaw = color.getRGB();
    }

}
