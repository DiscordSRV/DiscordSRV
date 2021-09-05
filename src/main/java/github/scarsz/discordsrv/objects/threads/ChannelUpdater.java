package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.api.entities.GuildChannel;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ChannelUpdater extends Thread {

    private final Set<UpdaterChannel> updaterChannels = new HashSet<>();

    public ChannelUpdater() {
        setName("DiscordSRV - Channel Updater");
    }

    public void reload() {
        // Deleting and recreating the list of updater channels
        this.updaterChannels.clear();

        final List<Map<String, String>> configEntries = DiscordSRV.config().getList("ChannelUpdater");
        for (final Map<String, String> configEntry : configEntries) {
            if (StringUtils.isBlank(configEntry.get("ChannelId"))) {
                DiscordSRV.error("Failed to initialise a channel updater entry: Channel ID is blank or null.");
            } else if (configEntry.get("ChannelId").equals("0000000000000000")) return; // Default value
        }
    }

    @Override
    public void run() {
        reload();
        while (true) {
            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Channel Updater thread: sleep interrupted");
                return;
            }
        }
    }

    private static class UpdaterChannel {

        private final GuildChannel channel;
        private final String format;
        private final int interval;
        private int minutesUntilRefresh;

        public UpdaterChannel(String channelId, String format, int interval) {
            this.channel = DiscordUtil.getJda().getGuildChannelById(channelId);
            this.format = format;
            this.interval = Math.max(interval, 10); // Minimum value is 10
            this.minutesUntilRefresh = this.interval;
        }
    }
}
