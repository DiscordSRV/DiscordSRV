package github.scarsz.discordsrv.objects.threads;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.UpdateUtil;

import java.util.concurrent.TimeUnit;

public class UpdateChecker extends Thread {

    public UpdateChecker() {
        super("DiscordSRV - Update Check");
    }

    public void check() {
        if (!DiscordSRV.isUpdateCheckDisabled()) {
            DiscordSRV.updateIsAvailable = UpdateUtil.checkForUpdates();
            DiscordSRV.updateChecked = true;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                // doesn't need to be frequent
                Thread.sleep(TimeUnit.HOURS.toMillis(6));
            } catch (InterruptedException e) {
                DiscordSRV.debug("Broke from Update check thread: sleep interrupted");
            }

            check();
        }
    }
}
