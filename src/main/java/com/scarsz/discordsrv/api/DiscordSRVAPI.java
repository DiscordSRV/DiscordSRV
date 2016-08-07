package com.scarsz.discordsrv.api;

import com.scarsz.discordsrv.DiscordSRV;

@SuppressWarnings("unused")
public class DiscordSRVAPI {

    public static void addListener(DiscordSRVListenerInterface listener) {
        DiscordSRV.listeners.add(listener);
    }
    public static void removeListener(DiscordSRVListenerInterface listener) {
        DiscordSRV.listeners.remove(listener);
    }

}
