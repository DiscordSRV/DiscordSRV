/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.PlayerBoostGuildEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.annotation.Nonnull;
import java.util.UUID;

public class DiscordBoostListener extends ListenerAdapter {
    @Override
    public void onGuildMemberUpdateBoostTime(@Nonnull GuildMemberUpdateBoostTimeEvent event) {
        final UUID linkedUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId());
        if (linkedUuid == null) {
            DiscordSRV.debug("Not handling boost for user " + event.getUser() + " because they didn't have a linked account");
            return;
        }
        if (!DiscordSRV.config().getBoolean("BoostSynchronizationDiscordToMinecraft")) {
            DiscordSRV.debug("Not handling boost for user " + event.getUser() + " because doing so is disabled in the config");
            return;
        }
        DiscordSRV.api.callEvent(new PlayerBoostGuildEvent(event));
    }
}
