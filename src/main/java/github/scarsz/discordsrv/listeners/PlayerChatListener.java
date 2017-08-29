package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        DiscordSRV.getPlugin().processChatMessage(event.getPlayer(), event.getMessage(), null, event.isCancelled());
    }

}
