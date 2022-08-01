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

import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.NamedValueFormatter;
import lombok.Getter;
import org.bukkit.OfflinePlayer;

import java.util.function.Function;

public class AccountLinkResult {

    public static AccountLinkResult alreadyLinked(OfflinePlayer player) {
        return new AccountLinkResult(Type.ALREADY_LINKED, key -> playerFormatter(key, player));
    }
    public static AccountLinkResult success(OfflinePlayer player) {
        return new AccountLinkResult(Type.SUCCESS, key -> playerFormatter(key, player));
    }
    public static AccountLinkResult unknownCode() {
        return new AccountLinkResult(Type.UNKNOWN_CODE, null);
    }
    public static AccountLinkResult invalidCode() {
        return new AccountLinkResult(Type.INVALID_CODE, null);
    }

    private static String playerFormatter(String key, OfflinePlayer player) {
        switch (key) {
            case "name":
            case "username":
                return player.getName() != null ? player.getName() : "<Unknown>";
            case "uuid":
                return player.getUniqueId().toString();
        }
        return null;
    }

    @Getter private final Type type;
    private final Function<String, Object> formatter;

    private AccountLinkResult(Type type, Function<String, Object> formatter) {
        this.type = type;
        this.formatter = formatter;
    }

    public String getMessage() {
        return formatter != null ? NamedValueFormatter.format(type.message.toString(), formatter) : type.message.toString();
    }

    public enum Type {

        ALREADY_LINKED(LangUtil.Message.ALREADY_LINKED),
        SUCCESS(LangUtil.Message.DISCORD_ACCOUNT_LINKED),
        UNKNOWN_CODE(LangUtil.Message.UNKNOWN_CODE),
        INVALID_CODE(LangUtil.Message.INVALID_CODE);

        @Getter private final LangUtil.Message message;

        Type(LangUtil.Message message) {
            this.message = message;
        }

    }

}
