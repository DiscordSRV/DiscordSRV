package com.scarsz.discordsrv.api;

import com.scarsz.discordsrv.api.events.ProcessChatEvent;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public interface DiscordSRVListener {

    void onDiscordMessageReceived(MessageReceivedEvent event);
    void onRawDiscordEventReceived(Event event);
    void onProcessChat(ProcessChatEvent event);

}
