package github.scarsz.discordsrv.objects.managers;

import com.google.gson.JsonObject;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.SQLUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@SuppressWarnings("SqlResolve")
public class JdbcAccountLinkManager extends AccountLinkManager {

    private final Connection connection;
    private final String database;
    private final String accountsTable;
    private final String codesTable;

    public static boolean shouldUseJdbc() {
        String jdbc = DiscordSRV.config().getString("Experiment_JdbcAccountLinkBackend");
        return StringUtils.isNotBlank(jdbc) && !jdbc.equals("jdbc:mysql//HOST:PORT/DATABASE?user=USERNAME&password=PASSWORD");
    }

    public JdbcAccountLinkManager() throws SQLException {
        String jdbc = DiscordSRV.config().getString("Experiment_JdbcAccountLinkBackend");
        if (!shouldUseJdbc() || StringUtils.isBlank(jdbc)) throw new RuntimeException("JDBC is not wanted");
        try {
            this.connection = DriverManager.getConnection(jdbc);
        } catch (SQLException e) {
            // check username for special characters
            try {
                if (!StringUtils.isAlphanumeric(Pattern.compile("user=(.+)&password=").matcher(jdbc).group(1)))
                    DiscordSRV.warning("JDBC user contains non-alphanumeric characters, be aware JDBC is a URL and that special characters such as & need to be URL-encoded to not break it");
            } catch (Exception e2) {}

            // check password for special characters
            try {
                if (!StringUtils.isAlphanumeric(Pattern.compile("&password=(.+)").matcher(jdbc).group(1)))
                    DiscordSRV.warning("JDBC password contains non-alphanumeric characters, be aware JDBC is a URL and that special characters such as & need to be URL-encoded to not break it");
            } catch (Exception e2) {}

            throw e;
        }
        database = connection.getCatalog();
        SQLUtil.createDatabaseIfNotExists(connection, database);
        accountsTable = database + ".accounts";
        codesTable = database + ".codes";

        if (SQLUtil.checkIfTableExists(connection, accountsTable)) {
            Map<String, String> expected = new HashMap<>();
            expected.put("discord", "varchar(32)");
            expected.put("uuid", "varchar(36)");
            if (!SQLUtil.checkIfTableMatchesStructure(connection, accountsTable, expected)) {
                throw new SQLException("JDBC table " + accountsTable + " does not match expected structure");
            }
        } else {
            connection.prepareStatement(
                    "create table " + accountsTable + "\n" +
                            "(\n" +
                            "    link    int auto_increment primary key,\n" +
                            "    discord varchar(32) not null,\n" +
                            "    uuid    varchar(36) not null,\n" +
                            "    constraint accounts_discord_uindex unique (discord),\n" +
                            "    constraint accounts_uuid_uindex unique (uuid)\n" +
                            ");"
            ).executeUpdate();
        }

        if (SQLUtil.checkIfTableExists(connection, codesTable)) {
            Map<String, String> expected = new HashMap<>();
            expected.put("code", "char(4)");
            expected.put("uuid", "varchar(36)");
            expected.put("expiration", "bigint(20)");
            if (!SQLUtil.checkIfTableMatchesStructure(connection, codesTable, expected)) {
                throw new SQLException("JDBC table " + codesTable + " does not match expected structure");
            }
        } else {
            connection.prepareStatement(
                    "create table " + codesTable + "\n" +
                            "(\n" +
                            "    code       char(4)     not null primary key,\n" +
                            "    uuid       varchar(36) not null,\n" +
                            "    expiration bigint(20)  not null,\n" +
                            "    constraint codes_uuid_uindex unique (uuid)\n" +
                            ");"
            ).executeUpdate();
        }

        DiscordSRV.info("JDBC tables passed validation, using JDBC account backend");

        File accountsFile = DiscordSRV.getPlugin().getLinkedAccountsFile();
        if (accountsFile.exists()) {
            try {
                if (DiscordSRV.getPlugin().getLinkedAccountsFile().length() != 0) {
                    DiscordSRV.info("linkedaccounts.json exists and we want to use JDBC backend, importing...");
                    File importFile = new File(accountsFile.getParentFile(), "linkedaccounts.json.imported");
                    if (!accountsFile.renameTo(importFile)) {
                        throw new RuntimeException("failed to move file to " + importFile.getName());
                    }

                    Map<String, UUID> accounts = new HashMap<>();
                    DiscordSRV.getPlugin().getGson().fromJson(FileUtils.readFileToString(importFile, Charset.forName("UTF-8")), JsonObject.class).entrySet().forEach(entry -> {
                        try {
                            accounts.put(entry.getKey(), UUID.fromString(entry.getValue().getAsString()));
                        } catch (Exception e) {
                            try {
                                accounts.put(entry.getValue().getAsString(), UUID.fromString(entry.getKey()));
                            } catch (Exception f) {
                                throw new RuntimeException("failed to parse");
                            }
                        }
                    });

                    connection.setAutoCommit(false);
                    for (Map.Entry<String, UUID> entry : accounts.entrySet()) {
                        String discord = entry.getKey();
                        UUID uuid = entry.getValue();

                        unlink(discord);
                        unlink(uuid);

                        PreparedStatement statement = connection.prepareStatement("insert into " + accountsTable + " (discord, uuid) VALUES (?, ?)");
                        statement.setString(1, discord);
                        statement.setString(2, uuid.toString());
                        statement.executeUpdate();
                    }
                    DiscordSRV.info("Imported " + accounts.size() + " accounts to JDBC, committing...");
                    connection.setAutoCommit(true); // commit all changes at once
                    DiscordSRV.info("Finished importing accounts to JDBC backend");
                } else {
                    DiscordSRV.getPlugin().getLinkedAccountsFile().delete();
                }
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    DiscordSRV.error("Failed to import linkedaccounts.json: " + e.getMessage());
                } else {
                    DiscordSRV.error("Failed to import linkedaccounts.json:");
                    e.printStackTrace();
                }
            }
        }
    }

    private void dropExpiredCodes() {
        try {
            PreparedStatement statement = connection.prepareStatement("delete from " + codesTable + " where `expiration` < ?");
            statement.setLong(1, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, UUID> getLinkingCodes() {
        dropExpiredCodes();

        Map<String, UUID> codes = new HashMap<>();

        try {
            PreparedStatement statement = connection.prepareStatement("select * from " + codesTable);
            ResultSet result = statement.executeQuery();

            while (result.next()) {
                codes.put(result.getString("code"), UUID.fromString(result.getString("uuid")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return codes;
    }

    @Override
    public Map<String, UUID> getLinkedAccounts() {
        Map<String, UUID> accounts = new HashMap<>();

        try {
            PreparedStatement statement = connection.prepareStatement("select * from " + accountsTable);
            ResultSet result = statement.executeQuery();

            while (result.next()) {
                accounts.put(result.getString("discord"), UUID.fromString(result.getString("uuid")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return accounts;
    }

    @Override
    public String generateCode(UUID playerUuid) {
        String code;
        do {
            int numbers = DiscordSRV.getPlugin().getRandom().nextInt(10000);
            code = String.format("%04d", numbers);
        } while (getLinkingCodes().containsKey(code));

        try {
            PreparedStatement statement = connection.prepareStatement("insert into " + codesTable + " (`code`, `uuid`, `expiration`) VALUES (?, ?, ?)");
            statement.setString(1, code);
            statement.setString(2, playerUuid.toString());
            statement.setLong(3, System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return code;
    }

    @Override
    public String process(String code, String discordId) {
        // strip the code to get rid of non-numeric characters
        code = code.replaceAll("[^0-9]", "");

        UUID uuid = getLinkingCodes().get(code);
        if (uuid != null) {
            link(discordId, uuid);

            try {
                PreparedStatement statement = connection.prepareStatement("delete from " + codesTable + " where `code` = ?");
                statement.setString(1, code);
                statement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            if (player.isOnline()) {
                player.getPlayer().sendMessage(LangUtil.Message.MINECRAFT_ACCOUNT_LINKED.toString()
                        .replace("%username%", DiscordUtil.getUserById(discordId).getName())
                        .replace("%id%", DiscordUtil.getUserById(discordId).getId())
                );
            }

            return LangUtil.Message.DISCORD_ACCOUNT_LINKED.toString()
                    .replace("%name%", player.getName())
                    .replace("%uuid%", getUuid(discordId).toString());
        }

        return code.length() == 4
                ? LangUtil.InternalMessage.UNKNOWN_CODE.toString()
                : LangUtil.InternalMessage.INVALID_CODE.toString();
    }

    @Override
    public String getDiscordId(UUID uuid) {
        try {
            PreparedStatement statement = connection.prepareStatement("select discord from " + accountsTable + " where uuid = ?");
            statement.setString(1, uuid.toString());
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString("discord");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public UUID getUuid(String discord) {
        try {
            PreparedStatement statement = connection.prepareStatement("select uuid from " + accountsTable + " where discord = ?");
            statement.setString(1, discord);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return UUID.fromString(result.getString("uuid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void link(String discordId, UUID uuid) {
        try {
            unlink(discordId);
            unlink(uuid);

            PreparedStatement statement = connection.prepareStatement("insert into " + accountsTable + " (discord, uuid) VALUES (?, ?)");
            statement.setString(1, discordId);
            statement.setString(2, uuid.toString());
            statement.executeUpdate();

            afterLink(discordId, uuid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void unlink(UUID uuid) {
        String discord = getDiscordId(uuid);
        if (discord == null) return;

        beforeUnlink(uuid, discord);
        try {
            PreparedStatement statement = connection.prepareStatement("delete from " + accountsTable + " where `uuid` = ?");
            statement.setString(1, uuid.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        afterUnlink(uuid, discord);
    }

    @Override
    public void unlink(String discordId) {
        UUID uuid = getUuid(discordId);
        if (uuid == null) return;

        beforeUnlink(uuid, discordId);
        try {
            PreparedStatement statement = connection.prepareStatement("delete from " + accountsTable + " where `discord` = ?");
            statement.setString(1, discordId);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        afterUnlink(uuid, discordId);
    }

    @Override
    public void save() {
        try {
            if (!connection.getAutoCommit()) {
                connection.commit();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
