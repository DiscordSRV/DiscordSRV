package com.scarsz.discordsrv.api.events;

import org.bukkit.entity.Player;

public class ProcessChatEvent {

    public Boolean isCancelled;
    public Player sender;
    public String message;
    public String channel;

}
