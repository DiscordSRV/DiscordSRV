package github.scarsz.discordsrv.hooks.chat;

import github.scarsz.discordsrv.hooks.PluginHook;

public interface ChatHook extends PluginHook {

    void broadcastMessageToChannel(String channel, String message);

}
