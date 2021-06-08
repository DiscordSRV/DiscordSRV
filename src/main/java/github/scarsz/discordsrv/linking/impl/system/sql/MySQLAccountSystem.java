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

package github.scarsz.discordsrv.linking.impl.system.sql;

import com.mysql.jdbc.Driver;
import github.scarsz.discordsrv.linking.AccountSystem;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;

/**
 * A <strong>MySQL</strong> SQL-backed {@link AccountSystem}.
 */
public class MySQLAccountSystem extends CachedSqlAccountSystem {

    @Getter private final Connection connection;

    /**
     * Creates a MySQL SQL-backed account system utilizing a file-based H2 database
     * @param host the host of the MySQL server
     * @param port the port of the MySQL server
     * @param database the database to be accessed
     * @param username the username to authenticate with
     * @param password the password for the given database user
     */
    public MySQLAccountSystem(String host, int port, String database, String username, String password) throws SQLException {
        Driver mysqlDriver = new Driver();
        Properties properties = new Properties();
        if (StringUtils.isNotBlank(username)) properties.put("user", username);
        if (StringUtils.isNotBlank(password)) properties.put("password", password);
        this.connection = mysqlDriver.connect(String.format(Locale.ROOT, "jdbc:mysql://%s:%d/%s", host, port, database), properties);
        initialize(database);
    }
    /**
     * Creates a MySQL SQL-backed account system utilizing a file-based H2 database
     * @param connectionUrl the raw connection URL to use
     */
    public MySQLAccountSystem(String connectionUrl) throws SQLException {
        this.connection = new Driver().connect(connectionUrl, new Properties());
        initialize(connection.getCatalog());
    }
    /**
     * Creates a MySQL SQL-backed account system utilizing the given {@link Connection}
     * @param connection the connection to use
     */
    public MySQLAccountSystem(Connection connection) throws SQLException {
        this.connection = connection;
        initialize(connection.getCatalog());
    }

    @Override
    public boolean canStoreNativeUuids() {
        return false;
    }

}
