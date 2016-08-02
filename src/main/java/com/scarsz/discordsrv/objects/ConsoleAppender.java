package com.scarsz.discordsrv.objects;

import com.scarsz.discordsrv.DiscordSRV;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.util.Date;

@SuppressWarnings("Duplicates")
@Plugin(name = "DiscordSRV-ConsoleChannel", category = "Core", elementType = "appender", printObject = true)
public class ConsoleAppender extends AbstractAppender {

    private String message = "";
    private Long millisAtNextSend = System.currentTimeMillis() + DiscordSRV.plugin.getConfig().getInt("DiscordConsoleChannelLogRefreshRate");

    public ConsoleAppender() {
        //super("DiscordSRV-ConsoleChannel", null, PatternLayout.createLayout("[%d{HH:mm:ss} %level]: %msg", null, null, null, null), false);
        super("DiscordSRV-ConsoleChannel", null, PatternLayout.createLayout("[%d{HH:mm:ss} %level]: %msg", null, null, null, null), false);
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public void append(LogEvent e) {
        // return if console channel isn't available
        if (DiscordSRV.consoleChannel == null) return;

        String line = "[" + new Date() + "] [" + e.getLevel().name().toUpperCase() + "] " + e.getMessage().getFormattedMessage();

        // do nothing if line is blank before parsing
        if (!lineIsOk(line)) return;

        // apply regex to line
        line = applyRegex(line);

        // do nothing if line is blank after parsing
        if (!lineIsOk(line)) return;

        // if line contains a blocked phrase don't send it
        Boolean shouldSkip = false;
        for (String phrase : DiscordSRV.plugin.getConfig().getStringList("DiscordConsoleChannelDoNotSendPhrases"))
            if (line.contains(phrase)) shouldSkip = true;
        if (shouldSkip) return;

        message += line + "\n";

        if (System.currentTimeMillis() - millisAtNextSend > 0L) {
            // past time to send messages

            // reset timer
            millisAtNextSend = System.currentTimeMillis() + DiscordSRV.plugin.getConfig().getInt("DiscordConsoleChannelLogRefreshRate");

            // send built message
            DiscordSRV.sendMessage(DiscordSRV.consoleChannel, message);
        }
    }

    private Boolean lineIsOk(String input) {
        return !input.replace(" ", "").replace("\n", "").isEmpty();
    }
    private String applyRegex(String input) {
        return input.replaceAll(DiscordSRV.plugin.getConfig().getString("DiscordConsoleChannelRegexFilter"), DiscordSRV.plugin.getConfig().getString("DiscordConsoleChannelRegexReplacement"));
    }

}