package github.scarsz.discordsrv.api.events;

import github.scarsz.discordsrv.util.DiscordUtil;
import lombok.Getter;
import net.dv8tion.jda.api.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * <p>Called directly after an account pair is unlinked via DiscordSRV's {@link github.scarsz.discordsrv.objects.managers.AccountLinkManager}</p>
 */
public class AccountUnlinkedEvent extends Event {

    @Getter private final OfflinePlayer player;
    @Getter private final String discordId;
    @Getter private final User discordUser;

    public AccountUnlinkedEvent(String discordId, UUID playerUuid) {
        this.player = Bukkit.getOfflinePlayer(playerUuid);
        this.discordId = discordId;
        this.discordUser = DiscordUtil.getUserById(discordId);
    }

}
