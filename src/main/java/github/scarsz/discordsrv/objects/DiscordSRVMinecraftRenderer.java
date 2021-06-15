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

/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2021 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.objects;

import dev.vankka.mcdiscordreserializer.renderer.implementation.DefaultMinecraftRenderer;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public class DiscordSRVMinecraftRenderer extends DefaultMinecraftRenderer {

    @Override
    public @Nullable Component appendEmoteMention(@NonNull Component component, @NonNull String name, @NonNull String id) {
        String behavior = DiscordSRV.config().getString("DiscordChatChannelEmoteBehavior");
        return behavior.equalsIgnoreCase("name")
                ? component.append(Component.text(":" + name + ":"))
                : component;
    }

    @Override
    public @Nullable Component appendChannelMention(@NonNull Component component, @NonNull String id) {
        TextChannel textChannel = DiscordUtil.getTextChannelById(id);
        if (textChannel != null) component = component.append(Component.text("#" + textChannel.getName()));
        return component;
    }

    @Override
    public @Nullable Component appendUserMention(@NonNull Component component, @NonNull String id) {
        Member member = DiscordUtil.getMemberById(id);
        if (member != null) component = component.append(Component.text("@" + member.getEffectiveName()));
        return component;
    }

    @Override
    public @Nullable Component appendRoleMention(@NonNull Component component, @NonNull String id) {
        Role role = null;
        try {
            role = DiscordUtil.getJda().getRoleById(id);
        } catch (Throwable ignored) {}
        if (role != null) component = component.append(Component.text("@" + role.getName()));
        return component;
    }
}
