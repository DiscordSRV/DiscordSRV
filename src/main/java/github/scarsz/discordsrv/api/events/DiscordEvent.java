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

package github.scarsz.discordsrv.api.events;

import net.dv8tion.jda.api.JDA;

/**
 * <p>The superclass of all Discord-related events</p>
 * <p>Provides {@link #getJda()} and {@link #getRawEvent()}</p>
 */
@SuppressWarnings("LombokGetterMayBeUsed")
abstract class DiscordEvent<T> extends Event {

    private final JDA jda;
    private final T rawEvent;

    DiscordEvent(JDA jda) {
        this.jda = jda;
        this.rawEvent = null;
    }

    DiscordEvent(JDA jda, T rawEvent) {
        this.jda = jda;
        this.rawEvent = rawEvent;
    }

    public JDA getJda() {
        return this.jda;
    }

    public T getRawEvent() {
        return this.rawEvent;
    }
}
