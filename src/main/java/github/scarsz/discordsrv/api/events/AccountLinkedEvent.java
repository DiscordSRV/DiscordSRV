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

package github.scarsz.discordsrv.api.events;

import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import java.util.UUID;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/**
 * <p>Called directly after an account pair is linked via DiscordSRV's {@link AccountLinkManager}</p>
 */
public class AccountLinkedEvent extends Event {

    private final OfflinePlayer player;
    private final User user;

    public AccountLinkedEvent(User user, UUID playerUuid) {
        this.player = Bukkit.getOfflinePlayer(playerUuid);
        this.user = user;
    }

    public OfflinePlayer getPlayer() {
        return this.player;
    }

    public User getUser() {
        return this.user;
    }
}
