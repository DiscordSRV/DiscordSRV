package github.scarsz.discordsrv.objects;

import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import lombok.Data;

@Data
public class ConsoleMessage {
    private final String timestamp;
    private final String level;
    private final String line;

    @Override
    public String toString() {
        return PlaceholderUtil.replacePlaceholdersToDiscord(LangUtil.Message.CONSOLE_CHANNEL_LINE.toString())
                .replace("%date%", timestamp)
                .replace("%level%", level)
                .replace("%line%", line);
    }
}
