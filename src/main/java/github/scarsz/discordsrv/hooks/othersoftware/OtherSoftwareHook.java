package github.scarsz.discordsrv.hooks.othersoftware;

import github.scarsz.discordsrv.hooks.PluginHook;

//For other message receiving software such as dynmap
//Not sure if you want to make other non plugin hooks, for things in the config
public interface OtherSoftwareHook extends PluginHook {
    void broadcast(String message);
}
