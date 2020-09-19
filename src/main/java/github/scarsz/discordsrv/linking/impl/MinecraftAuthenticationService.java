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
