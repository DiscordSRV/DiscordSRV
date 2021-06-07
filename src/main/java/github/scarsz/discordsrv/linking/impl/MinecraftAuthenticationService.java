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

package github.scarsz.discordsrv.linking.impl;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.linking.provider.AccountProvider;
import github.scarsz.discordsrv.linking.provider.DiscordAccountProvider;
import github.scarsz.discordsrv.linking.provider.MinecraftAccountProvider;
import lombok.NonNull;
import me.minecraftauth.lib.AuthService;
import me.minecraftauth.lib.account.AccountType;
import me.minecraftauth.lib.account.platform.discord.DiscordAccount;
import me.minecraftauth.lib.account.platform.minecraft.MinecraftAccount;
import me.minecraftauth.lib.exception.LookupException;

import java.util.UUID;

/**
 * An {@link AccountProvider} utilizing the <a href="https://minecraftauth.me">https://minecraftauth.me</a> service.
 */
public class MinecraftAuthenticationService implements DiscordAccountProvider, MinecraftAccountProvider {

    @Override
    public String getDiscordId(@NonNull UUID playerUuid) {
        try {
            return AuthService.lookup(AccountType.MINECRAFT, playerUuid, AccountType.DISCORD)
                    .map(account -> (DiscordAccount) account)
                    .map(DiscordAccount::getUserId)
                    .orElse(null);
        } catch (LookupException e) {
            DiscordSRV.error("[" + getClass().getSimpleName() + "] Request to convert Minecraft UUID " + playerUuid + " to Discord UID failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    public UUID getUuid(@NonNull String discordId) {
        try {
            return AuthService.lookup(AccountType.DISCORD, discordId, AccountType.MINECRAFT)
                    .map(account -> (MinecraftAccount) account)
                    .map(MinecraftAccount::getUUID)
                    .orElse(null);
        } catch (LookupException e) {
            DiscordSRV.error("[" + getClass().getSimpleName() + "] Request to convert Discord UID " + discordId + " to Minecraft UUID failed: " + e.getMessage());
            return null;
        }
    }

}
