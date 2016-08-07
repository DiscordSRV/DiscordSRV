package com.scarsz.discordsrv.threads;

import com.scarsz.discordsrv.DiscordSRV;

import java.util.List;

public class ConsoleMessageQueueWorker extends Thread {

    public void run() {
        while (!isInterrupted()) {

            String message = "";
            List<String> queueCopy = DiscordSRV.messageQueue;
            DiscordSRV.messageQueue.clear();
            for (String line : queueCopy) {
                if (message.length() + line.length() + 1 > 2000) {
                    DiscordSRV.sendMessageToConsoleChannel(message);
                    message = "";
                }
                message += line + "\n";
            }
            if (!"".equals(message.replace(" ", "").replace("\n", "")))
                DiscordSRV.sendMessageToConsoleChannel(message);

            try { Thread.sleep(DiscordSRV.plugin.getConfig().getInt("DiscordConsoleChannelLogRefreshRate")); } catch (Exception ignored) {}
        }
    }

}
