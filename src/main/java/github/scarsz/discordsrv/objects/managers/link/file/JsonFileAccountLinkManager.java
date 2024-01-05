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

package github.scarsz.discordsrv.objects.managers.link.file;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Deprecated
public class JsonFileAccountLinkManager extends AbstractFileAccountLinkManager {

    public JsonFileAccountLinkManager() {
        super();
    }

    void load() throws IOException {
        File file = getFile();
        if (!file.exists()) return;
        String fileContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        if (fileContent == null || StringUtils.isBlank(fileContent)) fileContent = "{}";
        JsonObject jsonObject;
        try {
            jsonObject = DiscordSRV.getPlugin().getGson().fromJson(fileContent, JsonObject.class);
        } catch (Throwable t) {
            if (!(t instanceof MalformedJsonException) && !(t instanceof JsonSyntaxException) || !t.getMessage().contains("JsonPrimitive")) {
                DiscordSRV.error("Failed to load " + file.getName(), t);
                return;
            } else {
                jsonObject = new JsonObject();
            }
        }

        jsonObject.entrySet().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();
            if (key.isEmpty() || value.isEmpty()) {
                // empty values are not allowed.
                return;
            }

            try {
                linkedAccounts.put(key, UUID.fromString(value));
            } catch (Exception e) {
                try {
                    linkedAccounts.put(value, UUID.fromString(key));
                } catch (Exception f) {
                    DiscordSRV.warning("Failed to load " + file.getName() + " file. It's extremely recommended to delete your " + file.getName() + " file.");
                }
            }
        });
    }

    @Override
    public void save() {
        long startTime = System.currentTimeMillis();

        try {
            JsonObject map = new JsonObject();
            synchronized (linkedAccounts) {
                linkedAccounts.forEach((discordId, uuid) -> map.addProperty(discordId, String.valueOf(uuid)));
            }
            FileUtils.writeStringToFile(getFile(), map.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            DiscordSRV.error(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVE_FAILED + ": " + e.getMessage());
            return;
        }

        DiscordSRV.info(LangUtil.InternalMessage.LINKED_ACCOUNTS_SAVED.toString()
                .replace("{ms}", String.valueOf(System.currentTimeMillis() - startTime))
        );
    }

    @Override
    File getFile() {
        return new File(DiscordSRV.getPlugin().getDataFolder(), "linkedaccounts.json");
    }

}
