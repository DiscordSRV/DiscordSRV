package github.scarsz.discordsrv.hooks.world;

import github.scarsz.discordsrv.hooks.PluginHook;

public interface WorldHook extends PluginHook {

    /**
     * Gets the alias for the given world
     *
     * @param world The name of the world to get the alias for
     * @return The world's alias or the provided string if no alias was found
     */
    String getWorldAlias(String world);
}