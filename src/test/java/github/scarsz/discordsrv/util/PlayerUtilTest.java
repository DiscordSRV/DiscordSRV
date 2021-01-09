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

package github.scarsz.discordsrv.util;

import org.junit.Test;

import static github.scarsz.discordsrv.util.PlayerUtil.convertTargetSelectors;
import static org.junit.Assert.assertEquals;

public class PlayerUtilTest {

    @Test
    public void parseValidTargetSelectors() {
        assertEquals("Is it {TARGET} selector ?", convertTargetSelectors("Is it @a selector ?", null));
        assertEquals("Nested {TARGET}{TARGET}", convertTargetSelectors("Nested @e@r[limit=2]", null));
        assertEquals("End {TARGET}", convertTargetSelectors("End @r", null));
    }

    @Test
    public void parseInvalidTargetSelectors() {
        assertEquals("@e[team= @u @", convertTargetSelectors("@e[team= @u @", null));
        assertEquals("@everyone @ax", convertTargetSelectors("@everyone @ax", null));
    }
}
