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

package github.scarsz.discordsrv.linking;

import github.scarsz.discordsrv.linking.provider.AccountProvider;
import github.scarsz.discordsrv.linking.store.AccountStore;
import github.scarsz.discordsrv.linking.store.CodeStore;
import org.jetbrains.annotations.NotNull;

public interface AccountSystem extends AccountStore, AccountProvider, CodeStore {

    /**
     * Process the given code as it were presented by the given Discord user (represented by ID.)
     * This will cause #
     * @param code the linking code to verify with
     * @param discordId the discord account that is presenting the given linking code
     * @return the result after verifying the code
     */
    @NotNull AccountLinkResult process(String code, String discordId);

    /**
     * Close the underlying data connection, if one exists
     */
    void close();

}
