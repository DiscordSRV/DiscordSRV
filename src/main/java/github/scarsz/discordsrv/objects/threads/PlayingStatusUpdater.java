package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;

public class PlayingStatusUpdater extends Thread {
    public PlayingStatusUpdater() {
        setName("DiscordSRV - Status Updater");
    }

    private int lastStatus=0;
    @Override
    public void run() {
        while (true) {
            int rate;
            //Breaking change to config
            try{
                rate=DiscordSRV.config().getInt("StatusUpdateRateInMinutes");
            }
            catch(IllegalArgumentException e){
                DiscordSRV.error("\"StatusUpdaterRateInMinutes\" not found in config");
                rate=2;
            }
            if (rate < 1) rate = 1;

            if (DiscordUtil.getJda() != null) {
                String status;
                //Prioritize CyclingStatuses
                if(DiscordSRV.config().getOptionalStringList("CyclingStatuses").isPresent()) {
                    status = DiscordSRV.config().getStringList("CyclingStatuses").get(lastStatus);
                    lastStatus++;

                    //Lists are zero indexed, but List.size() isn't
                    if(lastStatus == DiscordSRV.config().getStringList("CyclingStatuses").size()-1)
                        lastStatus=0;
                }
                else
                    status = DiscordSRV.config().getString("DiscordGameStatus");

                if(!StringUtils.isEmpty(status))
                    DiscordUtil.setGameStatus(status);
                else
                    DiscordSRV.debug("Skipping status update cycle, status was null");
            } else {
                DiscordSRV.debug("Skipping status update cycle, JDA was null");
            }

            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(rate));
            } catch (InterruptedException e) {
                DiscordSRV.error("Broke from Status Updater thread: sleep interrupted");
                return;
            }
        }
    }
}
