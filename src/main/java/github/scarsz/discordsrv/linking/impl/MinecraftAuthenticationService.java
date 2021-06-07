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

import alexh.weak.Dynamic;
import com.github.kevinsawicki.http.HttpRequest;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.linking.provider.AccountProvider;
import github.scarsz.discordsrv.linking.provider.DiscordAccountProvider;
import github.scarsz.discordsrv.linking.provider.MinecraftAccountProvider;
import lombok.NonNull;
import org.json.simple.parser.JSONParser;

import java.util.UUID;

/**
 * An {@link AccountProvider} utilizing the <a href="https://minecraftauth.me">https://minecraftauth.me</a> service.
 */
public class MinecraftAuthenticationService implements DiscordAccountProvider, MinecraftAccountProvider {

    private static final JSONParser jsonParser = new JSONParser();

    @Override
    public String getDiscordId(@NonNull UUID player) {
        try {
            HttpRequest request = HttpRequest.get("https://minecraftauth.me/api/lookup/discord?minecraft=" + player).acceptJson();
            if (request.ok()) {
                Dynamic json = Dynamic.from(jsonParser.parse(request.body()));
                Dynamic identifier = json.dget("discord.identifier");
                return identifier.isPresent() ? identifier.convert().intoString() : null;
            } else if (request.code() != 404) {
                DiscordSRV.error("[" + getClass().getSimpleName() + "] Request to convert Minecraft UUID " + player + " to Discord UID failed: " + request.code() + " " + request.message());
            }
        } catch (Exception e) {
            DiscordSRV.error("[" + getClass().getSimpleName() + "] Request to convert Minecraft UUID " + player + " to Discord UID failed: " + e.getMessage());
        }
        return null;
    }

    @Override
    public UUID getUuid(@NonNull String userId) {
        try {
            HttpRequest request = HttpRequest.get("https://minecraftauth.me/api/lookup/minecraft?discord=" + userId).acceptJson();
            if (request.ok()) {
                Dynamic json = Dynamic.from(jsonParser.parse(request.body()));
                Dynamic identifier = json.dget("minecraft.identifier");
                return identifier.isPresent() ? UUID.fromString(identifier.convert().intoString()) : null;
            } else if (request.code() != 404) {
                DiscordSRV.error("[" + getClass().getSimpleName() + "] Request to convert Discord UID " + userId + " to Minecraft UUID failed: " + request.code() + " " + request.message());
            }
        } catch (Exception e) {
            DiscordSRV.error("[" + getClass().getSimpleName() + "] Request to convert Discord UID " + userId + " to Minecraft UUID failed: " + e.getMessage());
        }
        return null;
    }

}
