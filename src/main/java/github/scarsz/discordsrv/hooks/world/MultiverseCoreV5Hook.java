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

package github.scarsz.discordsrv.hooks.world;

import github.scarsz.discordsrv.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.plugin.Plugin;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;
import org.mvplugins.multiverse.external.vavr.control.Option;

public class MultiverseCoreV5Hook implements WorldHook {

    @Override
    public String getWorldAlias(String world) {
        MultiverseCoreApi mvApi = MultiverseCoreApi.get();
        WorldManager worldManager = mvApi.getWorldManager();

        Option<MultiverseWorld> optionalWorld = worldManager.getWorld(world);
        if (optionalWorld.isEmpty()) return world;

        MultiverseWorld mvWorld = optionalWorld.get();
        String alias = mvWorld.getAlias();
        return StringUtils.isNotBlank(alias) ? alias : world;
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("Multiverse-Core");
    }

    @Override
    public boolean isEnabled() {
        Plugin plugin = getPlugin();
        boolean enabled = plugin != null && plugin.isEnabled() && PluginUtil.pluginHookIsEnabled(plugin.getName());
        if (!enabled) return false;

        try {
            Class.forName("org.mvplugins.multiverse.core.MultiverseCoreApi");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
