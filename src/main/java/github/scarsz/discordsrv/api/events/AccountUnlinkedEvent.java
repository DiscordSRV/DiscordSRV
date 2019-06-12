package github.scarsz.discordsrv.api.events;

import lombok.Getter;
import net.dv8tion.jda.core.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * <p>Called directly after an account pair is unlinked via DiscordSRV's {@link AccountUnlinkedEvent}</p>
 */
public class AccountUnlinkedEvent extends Event {

    @Getter private final OfflinePlayer player;
    @Getter private final User user;

    public AccountUnlinkedEvent(User user, UUID playerUuid) {
        this.player = Bukkit.getOfflinePlayer(playerUuid);
        this.user = user;
    }
}
