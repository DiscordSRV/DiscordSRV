package github.scarsz.discordsrv.objects;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 5:22 PM
 */
@SuppressWarnings("unchecked")
@Plugin(name = "DiscordSRV-ConsoleChannel", category = "Core", elementType = "appender", printObject = true)
public class ConsoleAppender extends AbstractAppender {

    public ConsoleAppender() {
        super("DiscordSRV-ConsoleChannel", null, PatternLayout.createLayout("[%d{HH:mm:ss} %level]: %msg", null, null, null, null), false);
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public void append(LogEvent e) {
        // return if console channel isn't available
        if (DiscordSRV.getPlugin().getConsoleChannel() == null) return;

        // return if this is not an okay level to send
        boolean isAnOkayLevel = false;
        for (String consoleLevel : DiscordSRV.getPlugin().getConfig().getStringList("DiscordConsoleChannelLevels")) if (consoleLevel.toLowerCase().equals(e.getLevel().name().toLowerCase())) isAnOkayLevel = true;
        if (!isAnOkayLevel) return;

        String line = e.getMessage().getFormattedMessage();

        // do nothing if line is blank before parsing
        if (!lineIsOk(line)) return;

        // apply regex to line
        line = applyRegex(line);

        // do nothing if line is blank after parsing
        if (!lineIsOk(line)) return;

        // apply formatting
        line = DiscordSRV.getPlugin().getConfig().getString("DiscordConsoleChannelFormat")
                .replace("%date%", TimeUtil.timeStamp())
                .replace("%level%", e.getLevel().name().toUpperCase())
                .replace("%line%", line)
        ;

        // if line contains a blocked phrase don't send it
        boolean doNotSendActsAsWhitelist = DiscordSRV.getPlugin().getConfig().getBoolean("DiscordConsoleChannelDoNotSendPhrasesActsAsWhitelist");
        for (String phrase : DiscordSRV.getPlugin().getConfig().getStringList("DiscordConsoleChannelDoNotSendPhrases"))
            if (line.contains(phrase) == !doNotSendActsAsWhitelist) return;

        // remove coloring shit
        line = DiscordUtil.stripColor(line);

        // queue final message
        DiscordSRV.getPlugin().getConsoleMessageQueue().add(line);
    }

    private boolean lineIsOk(String input) {
        return input != null && !input.replace(" ", "").replace("\n", "").isEmpty();
    }
    private String applyRegex(String input) {
        return input.replaceAll(DiscordSRV.getPlugin().getConfig().getString("DiscordConsoleChannelRegexFilter"), DiscordSRV.getPlugin().getConfig().getString("DiscordConsoleChannelRegexReplacement"));
    }

}