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
    public void setLinkedDiscord(@NonNull UUID uuid, @Nullable String discordId) {
        if (discordId != null) {
            if (isLinked(uuid)) {
                try (PreparedStatement statement = connection.prepareStatement("update `discordsrv.accounts` set discord = ? where uuid = ?")) {
                    statement.setString(1, discordId);
                    statement.setObject(2, uuid);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("insert into `discordsrv.accounts` (discord, uuid) values (?, ?)")) {
                    statement.setString(1, discordId);
                    statement.setObject(2, uuid);
                    statement.executeUpdate();
                }
            }
            callAccountLinkedEvent(discordId, uuid);
        } else {
            String previousDiscordId = getDiscordId(uuid);
            try (PreparedStatement statement = connection.prepareStatement("delete from `discordsrv.accounts` where uuid = ?")) {
                statement.setObject(1, uuid);
                statement.executeUpdate();
            }
            if (previousDiscordId != null) {
                callAccountUnlinkedEvent(previousDiscordId, uuid);
            }
        }
    }

    @Override
    @SneakyThrows
    public void setLinkedMinecraft(@NonNull String discordId, @Nullable UUID uuid) {
        if (uuid != null) {
            if (isLinked(discordId)) {
                try (PreparedStatement statement = connection.prepareStatement("update `discordsrv.accounts` set uuid = ? where discord = ?")) {
                    statement.setObject(1, uuid);
                    statement.setString(2, discordId);
                    statement.executeUpdate();
                }
            } else {
                try (PreparedStatement statement = connection.prepareStatement("insert into `discordsrv.accounts` (discord, uuid) values (?, ?)")) {
                    statement.setString(1, discordId);
                    statement.setObject(2, uuid);
                    statement.executeUpdate();
                }
            }
            callAccountLinkedEvent(discordId, uuid);
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
    public void storeLinkingCode(@NonNull String code, @NonNull UUID uuid) {
        try (PreparedStatement statement = connection.prepareStatement("insert into `discordsrv.codes` (code, uuid) values (?, ?)")) {
            statement.setString(1, code);
            statement.setObject(2, uuid);
            statement.executeUpdate();
        }
    }

}
