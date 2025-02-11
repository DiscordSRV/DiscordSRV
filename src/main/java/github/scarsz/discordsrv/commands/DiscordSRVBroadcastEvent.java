package github.scarsz.discordsrv.commands;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.command.CommandSender;

/*
 *  This is a custom event listener for bcast/broadcast, so you can capture alerts and customize it to your own content.
 * 
 */
public class DiscordSRVBroadcastEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final CommandSender sender;
    private final String message;

    public DiscordSRVBroadcastEvent(CommandSender sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public CommandSender getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}