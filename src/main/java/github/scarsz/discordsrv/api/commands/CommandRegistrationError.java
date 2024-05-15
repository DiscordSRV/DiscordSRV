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

package github.scarsz.discordsrv.api.commands;

import java.util.Objects;
import net.dv8tion.jda.api.entities.Guild;

public final class CommandRegistrationError {

    private final Guild guild;
    private final Throwable exception;

    public CommandRegistrationError(Guild guild, Throwable exception) {
        this.guild = guild;
        this.exception = exception;
    }

    public Guild getGuild() {
        return this.guild;
    }

    public Throwable getException() {
        return this.exception;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof CommandRegistrationError)) return false;
        if (!Objects.equals(this.getGuild(), ((CommandRegistrationError) o).getGuild())) return false;
        return Objects.equals(this.getException(), ((CommandRegistrationError) o).getException());
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.getGuild() == null ? 43 : this.getGuild().hashCode());
        result = result * PRIME + (this.getException() == null ? 43 : this.getException().hashCode());
        return result;
    }

    public String toString() {
        return "CommandRegistrationError(guild=" + this.getGuild() + ", exception=" + this.getException() + ")";
    }
}
