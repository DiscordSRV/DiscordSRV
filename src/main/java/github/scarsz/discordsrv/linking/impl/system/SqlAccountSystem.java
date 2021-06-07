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

package github.scarsz.discordsrv.linking.impl.system;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.linking.AccountSystem;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

import javax.annotation.Nullable;
import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A SQL-backed {@link AccountSystem}.
 */
public class SqlAccountSystem extends BaseAccountSystem {

    @Getter private final Connection connection;

    /**
     * Creates a SQL-backed account system utilizing a file-based H2 database
     * @param database the database file to use
     */
    @SneakyThrows
    public SqlAccountSystem(File database) {
        connection = DriverManager.getConnection("jdbc:h2:" + database.getAbsolutePath());
    }
    /**
     * Creates a SQL-backed account system utilizing the given {@link Connection}
     * @param connection the connection to use
     */
    public SqlAccountSystem(Connection connection) {
        this.connection = connection;
    }

    @Override
    @SneakyThrows
    public String getDiscordId(@NonNull UUID player) {
        try (PreparedStatement statement = connection.prepareStatement("select discord from `discordsrv.accounts` where uuid = ?")) {
            statement.setObject(1, player);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString("discord");
            }
            return null;
        } catch (SQLException e) {
            DiscordSRV.error("[" + getClass().getSimpleName() + "] Converting Minecraft UUID " + player + " to Discord UID failed: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @SneakyThrows
    public UUID getUuid(@NonNull String discordId) {
        try (PreparedStatement statement = connection.prepareStatement("select uuid from `discordsrv.accounts` where discord = ?")) {
            statement.setString(1, discordId);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return (UUID) result.getObject("uuid");
            }
            return null;
        } catch (SQLException e) {
            DiscordSRV.error("[" + getClass().getSimpleName() + "] Converting Discord UID " + discordId + " to Minecraft UUID failed: " + e.getMessage());
            throw e;
        }
    }

    @Override
    @SneakyThrows
    public void setLinkedDiscord(@NonNull UUID playerUuid, @Nullable String discordId) {
        if (discordId != null) {
            if (isLinked(playerUuid)) {
                try (PreparedStatement statement = connection.prepareStatement("update `discordsrv.accounts` set discord = ? where uuid = ?")) {
                    statement.setString(1, discordId);
                    statement.setObject(2, playerUuid);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("insert into `discordsrv.accounts` (discord, uuid) values (?, ?)")) {
                    statement.setString(1, discordId);
                    statement.setObject(2, playerUuid);
                    statement.executeUpdate();
                }
            }
            callAccountLinkedEvent(discordId, playerUuid);
        } else {
            String previousDiscordId = getDiscordId(playerUuid);
            try (PreparedStatement statement = connection.prepareStatement("delete from `discordsrv.accounts` where uuid = ?")) {
                statement.setObject(1, playerUuid);
                statement.executeUpdate();
            }
            if (previousDiscordId != null) {
                callAccountUnlinkedEvent(previousDiscordId, playerUuid);
            }
        }
    }

    @Override
    @SneakyThrows
    public void setLinkedMinecraft(@NonNull String discordId, @Nullable UUID playerUuid) {
        if (playerUuid != null) {
            if (isLinked(discordId)) {
                try (PreparedStatement statement = connection.prepareStatement("update `discordsrv.accounts` set uuid = ? where discord = ?")) {
                    statement.setObject(1, playerUuid);
                    statement.setString(2, discordId);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("insert into `discordsrv.accounts` (discord, uuid) values (?, ?)")) {
                    statement.setString(1, discordId);
                    statement.setObject(2, playerUuid);
                    statement.executeUpdate();
                }
            }
            callAccountLinkedEvent(discordId, playerUuid);
        } else {
            UUID previousPlayer = getUuid(discordId);
            try (PreparedStatement statement = connection.prepareStatement("delete from `discordsrv.accounts` where discord = ?")) {
                statement.setString(1, discordId);
                statement.executeUpdate();
            }
            if (previousPlayer != null) {
                callAccountUnlinkedEvent(discordId, previousPlayer);
            }
        }
    }

    @Override
    @SneakyThrows
    public UUID lookupCode(String code) {
        try (PreparedStatement statement = connection.prepareStatement("select uuid from `discordsrv.codes` where code = ?")) {
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return (UUID) result.getObject("uuid");
            }
        }
        return null;
    }

    @Override
    @SneakyThrows
    public @NonNull Map<String, UUID> getLinkingCodes() {
        try (PreparedStatement statement = connection.prepareStatement("select code, uuid from `discordsrv.codes`")) {
            ResultSet result = statement.executeQuery();
            Map<String, UUID> codes = new HashMap<>();
            while (result.next()) {
                codes.put(
                        result.getString("code"),
                        (UUID) result.getObject("uuid")
                );
            }
            return codes;
        }
    }

    @Override
    @SneakyThrows
    public void storeLinkingCode(@NonNull String code, @NonNull UUID playerUuid) {
        try (PreparedStatement statement = connection.prepareStatement("insert into `discordsrv.codes` (code, uuid) values (?, ?)")) {
            statement.setString(1, code);
            statement.setObject(2, playerUuid);
            statement.executeUpdate();
        }
    }

}
