package com.scarsz.discordsrv.api.events;

import org.bukkit.entity.Player;

@SuppressWarnings("WeakerAccess")
public class ProcessChatEvent {

    public Boolean isCancelled;
    public Player sender;
    public String message;
    public String channel;

    public ProcessChatEvent(Boolean isCancelled, Player sender, String message, String channel) {
        this.isCancelled = isCancelled;
        this.sender = sender;
        this.message = message;
        this.channel = channel;
    }

}
