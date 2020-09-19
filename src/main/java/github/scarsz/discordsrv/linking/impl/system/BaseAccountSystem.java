package github.scarsz.discordsrv.linking.impl.system;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.api.events.AccountUnlinkedEvent;
import github.scarsz.discordsrv.linking.AccountSystem;

import java.util.UUID;

public abstract class BaseAccountSystem implements AccountSystem {

    protected void callAccountLinkedEvent(String discordId, UUID player) {
        DiscordSRV.api.callEvent(new AccountLinkedEvent(discordId, player));
    }
    protected void callAccountUnlinkedEvent(String discordId, UUID player) {
        DiscordSRV.api.callEvent(new AccountUnlinkedEvent(discordId, player));
    }

}
