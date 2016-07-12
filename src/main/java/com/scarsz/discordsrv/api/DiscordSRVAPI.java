package com.scarsz.discordsrv.api;

import com.scarsz.discordsrv.DiscordSRV;

@SuppressWarnings("unused")
public class DiscordSRVAPI {

    public static void addListener(DiscordSRVListener listener) {
        DiscordSRV.listeners.add(listener);
    }
    public static void removeListener(DiscordSRVListener listener) {
        DiscordSRV.listeners.remove(listener);
    }

}
