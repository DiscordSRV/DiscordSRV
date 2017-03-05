package github.scarsz.discordsrv.api.events;

import github.scarsz.discordsrv.objects.AccountLinkManager;
import lombok.Getter;
import net.dv8tion.jda.core.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * <p>Called directly after an account pair is linked via DiscordSRV's {@link AccountLinkManager}</p>
 */
public class AccountLinkedEvent extends Event {

    @Getter private final OfflinePlayer player;
    @Getter private final User user;

    public AccountLinkedEvent(User user, UUID playerUuid) {
        this.player = Bukkit.getPlayer(playerUuid);
        this.user = user;
    }

}
