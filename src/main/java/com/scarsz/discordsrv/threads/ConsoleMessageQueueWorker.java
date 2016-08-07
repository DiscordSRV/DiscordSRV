package com.scarsz.discordsrv.threads;

import com.scarsz.discordsrv.DiscordSRV;

public class ConsoleMessageQueueWorker extends Thread {

    public void run() {
        while (!isInterrupted()) {

            String message = "";

            synchronized (DiscordSRV.messageQueue) {
                for (String line : DiscordSRV.messageQueue) {
                    if (message.length() + line.length() + 1 > 2000) {
                        DiscordSRV.sendMessageToConsoleChannel(message);
                        message = "";
                    }
                    message += line + "\n";
                }
                DiscordSRV.messageQueue.clear();
            }
            if (!"".equals(message.replace(" ", "").replace("\n", "")))
                DiscordSRV.sendMessageToConsoleChannel(message);

            try { Thread.sleep(DiscordSRV.plugin.getConfig().getInt("DiscordConsoleChannelLogRefreshRate")); } catch (Exception ignored) {}
        }
    }

}
