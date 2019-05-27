package github.scarsz.discordsrv.objects.managers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.SQLUtil;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JdbcAccountLinkManager extends AccountLinkManager {

    private final Connection connection;

    public static boolean shouldUseJdbc() {
        String jdbc = DiscordSRV.config().getString("Experiment_JdbcAccountLinkBackend");
        return StringUtils.isNotBlank(jdbc) && !jdbc.equals("jdbc:mysql//user:password@host:port/database");
    }

    public JdbcAccountLinkManager() throws SQLException {
        if (!shouldUseJdbc()) throw new RuntimeException("JDBC is not wanted");
        this.connection = DriverManager.getConnection(DiscordSRV.config().getString("Experiment_JdbcAccountLinkBackend"));
        String database = connection.getCatalog();
        SQLUtil.createDatabaseIfNotExists(connection, database);
        String accountsTable = database + ".accounts";
        String codesTable = database + ".codes";

        if (SQLUtil.checkIfTableExists(connection, accountsTable)) {
            Map<String, String> expected = new HashMap<>();
            expected.put("discord", "varchar(32)");
            expected.put("uuid", "varchar(36)");
            if (!SQLUtil.checkIfTableMatchesStructure(connection, accountsTable, expected)) {
                throw new SQLException("JDBC table " + accountsTable + " does not match expected structure");
            }
        } else {
            connection.prepareStatement(
                    "create table accounts\n" +
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
            expected.put("expiration", "bigint");
            if (!SQLUtil.checkIfTableMatchesStructure(connection, codesTable, expected)) {
                throw new SQLException("JDBC table " + codesTable + " does not match expected structure");
            }
        } else {
            connection.prepareStatement(
                    "create table codes\n" +
                            "(\n" +
                            "    code       char(4)     not null primary key,\n" +
                            "    uuid       varchar(36) not null,\n" +
                            "    expiration bigint      not null,\n" +
                            "    constraint codes_uuid_uindex unique (uuid)\n" +
                            ");"
            ).executeUpdate();
        }

        DiscordSRV.info("JDBC accounts table passed validation, using JDBC account backend");
    }

    @Override
    public Map<String, UUID> getLinkingCodes() {
        return super.getLinkingCodes();
    }

    @Override
    public Map<String, UUID> getLinkedAccounts() {
        return super.getLinkedAccounts();
    }

    @Override
    public String generateCode(UUID playerUuid) {
        return super.generateCode(playerUuid);
    }

    @Override
    public String process(String linkCode, String discordId) {
        return super.process(linkCode, discordId);
    }

    @Override
    public String getDiscordId(UUID uuid) {
        return super.getDiscordId(uuid);
    }

    @Override
    public UUID getUuid(String discordId) {
        return super.getUuid(discordId);
    }

    @Override
    public void link(String discordId, UUID uuid) {
        super.link(discordId, uuid);
    }

    @Override
    public void unlink(UUID uuid) {
        super.unlink(uuid);
    }

    @Override
    public void unlink(String discordId) {
        super.unlink(discordId);
    }

    @Override
    public void save() {
        super.save();
    }

}
