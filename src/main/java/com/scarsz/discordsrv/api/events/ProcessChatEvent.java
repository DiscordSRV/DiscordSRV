package com.scarsz.discordsrv.api.events;

import org.bukkit.entity.Player;

@SuppressWarnings("WeakerAccess")
public class ProcessChatEvent {

    public boolean isCancelled;
    public Player sender;
    public String message;
    public String channel;

    public ProcessChatEvent(boolean isCancelled, Player sender, String message, String channel) {
        this.isCancelled = isCancelled;
        this.sender = sender;
        this.message = message;
        this.channel = channel;
    }

}
