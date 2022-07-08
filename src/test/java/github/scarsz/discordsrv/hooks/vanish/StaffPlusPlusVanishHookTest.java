/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.hooks.vanish;

import net.shortninja.staffplusplus.IStaffPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicesManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StaffPlusPlusVanishHookTest {

    private static ServicesManager servicesManager;
    private static IStaffPlus staffPlusPlus;

    @Mock
    private Player player;

    @BeforeClass
    public static void setUp() {
        servicesManager = mock(ServicesManager.class, RETURNS_DEEP_STUBS);
        staffPlusPlus = mock(IStaffPlus.class, RETURNS_DEEP_STUBS);

        MockedStatic<Bukkit> bukkit = Mockito.mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getServicesManager).thenReturn(servicesManager);
        when(servicesManager.getRegistration(IStaffPlus.class).getProvider()).thenReturn(staffPlusPlus);
    }

    @Test
    public void givenStaffPlusPlusNotEnabled_thenReturnFalse () {
        when(servicesManager.getRegistration(IStaffPlus.class)).thenReturn(null);

        new StaffPlusPlusVanishHook().isVanished(player);

        assertFalse(new StaffPlusPlusVanishHook().isVanished(player));
    }

    @Test
    public void givenStaffPlusPlusEnabled_whenPlayerIsNotVanished_thenReturnFalse () {
        when(staffPlusPlus.getSessionManager().get(player).isVanished()).thenReturn(false);

        new StaffPlusPlusVanishHook().isVanished(player);

        assertFalse(new StaffPlusPlusVanishHook().isVanished(player));
    }

    @Test
    public void givenStaffPlusPlusEnabled_whenPlayerIsVanished_thenReturnTrue () {
        when(staffPlusPlus.getSessionManager().get(player).isVanished()).thenReturn(true);

        new StaffPlusPlusVanishHook().isVanished(player);

        assertFalse(new StaffPlusPlusVanishHook().isVanished(player));
    }
}
