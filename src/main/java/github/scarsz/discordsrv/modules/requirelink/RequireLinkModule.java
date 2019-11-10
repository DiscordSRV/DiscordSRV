package github.scarsz.discordsrv.modules.requirelink;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RequireLinkModule implements Listener {

    public RequireLinkModule() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    private void check(EventPriority priority, AsyncPlayerPreLoginEvent event) {
        String requestedPriority = DiscordSRV.config().getString("Require linked account to play.Listener priority");
        EventPriority targetPriority = Arrays.stream(EventPriority.values())
                .filter(p -> p.name().equalsIgnoreCase(requestedPriority))
                .findFirst().orElse(EventPriority.LOWEST);
        if (priority != targetPriority) return;

        try {
            if (!isEnabled()) return;
            if (getBypassNames().contains(event.getName())) return;
            if (checkWhitelist()) {
                boolean whitelisted = Bukkit.getServer().getWhitelistedPlayers().stream().map(OfflinePlayer::getUniqueId).anyMatch(u -> u.equals(event.getUniqueId()));
                if (whitelisted) {
                    DiscordSRV.debug("Player " + event.getName() + " is bypassing link requirement, player is whitelisted");
                    return;
                }
            }
            if (Bukkit.getServer().getBannedPlayers().stream().anyMatch(p -> p.getUniqueId().equals(event.getUniqueId()))) {
                DiscordSRV.debug("Player " + event.getName() + " is banned, skipping linked check");
                return;
            }
            if (Bukkit.getServer().getIPBans().stream().anyMatch(ip -> ip.equals(event.getAddress().getHostAddress()))) {
                DiscordSRV.debug("Player " + event.getName() + " connecting with banned IP " + event.getAddress().getHostAddress() + ", skipping linked check");
                return;
            }

            if (!DiscordSRV.isReady) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', getDiscordSRVStillStartingKickMessage()));
                return;
            }

            String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(event.getUniqueId());
            if (discordId == null) {
                Member botMember = DiscordSRV.getPlugin().getMainGuild().getSelfMember();
                String botName = botMember.getEffectiveName() + "#" + botMember.getUser().getDiscriminator();
                String code = DiscordSRV.getPlugin().getAccountLinkManager().generateCode(event.getUniqueId());

                event.disallow(
                        AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                        ChatColor.translateAlternateColorCodes('&', DiscordSRV.config().getString("Require linked account to play.Not linked message"))
                                .replace("{BOT}", botName)
                                .replace("{CODE}", code)
                );
                return;
            }


            List<String> subRoleIds = DiscordSRV.config().getStringList("Require linked account to play.Subscriber role.Subscriber roles");
            if (isSubRoleRequired() && !subRoleIds.isEmpty()) {
                int failedRoleIds = 0;
                int matches = 0;

                for (String subRoleId : subRoleIds) {
                    if (StringUtils.isBlank(subRoleId)) {
                        failedRoleIds++;
                        continue;
                    }

                    Role role = DiscordUtil.getJda().getRoleById(subRoleId);
                    if (role == null) {
                        failedRoleIds++;
                        continue;
                    }

                    Member member = role.getGuild().getMemberById(discordId);
                    if (member != null && member.getRoles().contains(role)) {
                        matches++;
                    }
                }

                if (failedRoleIds == subRoleIds.size()) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', getFailedToFindRoleKickMessage()));
                    return;
                }

                if (getAllSubRolesRequired() ? matches < subRoleIds.size() : matches == 0) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, ChatColor.translateAlternateColorCodes('&', getSubscriberRoleKickMessage()));
                }
            }
        } catch (Exception exception) {
            DiscordSRV.error("Failed to check player: " + event.getName());
            exception.printStackTrace();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.translateAlternateColorCodes('&', getUnknownFailureKickMessage()));
        }
    }

    public void noticePlayerUnlink(Player player) {
        if (!isEnabled()) return;
        if (getBypassNames().contains(player.getName())) return;
        if (checkWhitelist()) {
            boolean whitelisted = Bukkit.getServer().getWhitelistedPlayers().stream().map(OfflinePlayer::getUniqueId).anyMatch(u -> u.equals(player.getUniqueId()));
            if (whitelisted) {
                DiscordSRV.debug("Player " + player.getName() + " is bypassing link requirement, player is whitelisted");
                return;
            }
        }

        player.kickPlayer(ChatColor.translateAlternateColorCodes('&', getUnlinkedKickMessage()));
    }

    private boolean checkWhitelist() {
        return DiscordSRV.config().getBoolean("Require linked account to play.Whitelisted players bypass check");
    }
    private boolean getAllSubRolesRequired() {
        return DiscordSRV.config().getBoolean("Require linked account to play.Subscriber role.Require all of the listed roles");
    }
    private boolean isEnabled() {
        return DiscordSRV.config().getBoolean("Require linked account to play.Enabled");
    }
    private boolean isSubRoleRequired() {
        return DiscordSRV.config().getBoolean("Require linked account to play.Subscriber role.Require subscriber role to join");
    }
    private Set<String> getBypassNames() {
        return new HashSet<>(DiscordSRV.config().getStringList("Require linked account to play.Bypass names"));
    }
    private String getDiscordSRVStillStartingKickMessage() {
        return DiscordSRV.config().getString("Require linked account to play.Messages.DiscordSRV still starting");
    }
    private String getFailedToFindRoleKickMessage() {
        return DiscordSRV.config().getString("Require linked account to play.Messages.Failed to find subscriber role");
    }
    private String getSubscriberRoleKickMessage() {
        return DiscordSRV.config().getString("Require linked account to play.Subscriber role.Kick message");
    }
    private String getUnknownFailureKickMessage() {
        return DiscordSRV.config().getString("Require linked account to play.Messages.Failed for unknown reason");
    }
    private String getUnlinkedKickMessage() {
        return DiscordSRV.config().getString("Require linked account to play.Messages.Kicked for unlinking");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEventLowest(AsyncPlayerPreLoginEvent event) {
        check(EventPriority.LOWEST, event);
    }
    @EventHandler(priority = EventPriority.LOW)
    public void onEventLow(AsyncPlayerPreLoginEvent event) {
        check(EventPriority.LOW, event);
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEventNormal(AsyncPlayerPreLoginEvent event) {
        check(EventPriority.NORMAL, event);
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onEventHigh(AsyncPlayerPreLoginEvent event) {
        check(EventPriority.HIGH, event);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEventHighest(AsyncPlayerPreLoginEvent event) {
        check(EventPriority.HIGHEST, event);
    }

}
