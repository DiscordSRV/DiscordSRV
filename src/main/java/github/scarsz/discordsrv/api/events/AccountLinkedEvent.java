/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.api.events;

import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import lombok.Getter;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * <p>Called directly after an account pair is linked via DiscordSRV's {@link AccountLinkManager}</p>
 */
public class AccountLinkedEvent extends Event {

    @Getter private final OfflinePlayer player;
    @Getter private final User user;

    public AccountLinkedEvent(User user, UUID playerUuid) {
        this.player = Bukkit.getPlayer(playerUuid);
        this.user = user;
    }

}
