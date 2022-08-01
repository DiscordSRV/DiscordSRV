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

import github.scarsz.discordsrv.linking.AccountSystem;
import lombok.Getter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A <strong>H2</strong> SQL-backed {@link AccountSystem}.
 */
public class H2AccountSystem extends SqlAccountSystem {

    @Getter private final Connection connection;

    /**
     * Creates a H2 SQL-backed account system utilizing a file-based H2 database
     * @param database the database file to use
     */
    public H2AccountSystem(File database) throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:" + database.getAbsolutePath());
        initialize(connection.getCatalog());
    }
    /**
     * Creates a H2 SQL-backed account system utilizing the given {@link Connection}
     * @param connection the connection to use
     */
    public H2AccountSystem(Connection connection) throws SQLException {
        this.connection = connection;
        initialize(connection.getCatalog());
    }

    @Override
    public boolean canStoreNativeUuids() {
        return true;
    }

}
