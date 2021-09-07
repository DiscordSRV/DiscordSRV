/*-
 * LICENSE
 * DiscordSRV
 * -------------
 * Copyright (C) 2016 - 2021 Austin "Scarsz" Shapiro
 * -------------
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * END
 */

package github.scarsz.discordsrv.modules.requirelink;

import alexh.weak.Dynamic;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.*;
import java.util.function.BiConsumer;

public class RequireLinkModule implements Listener {

    public RequireLinkModule() {
        Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
    }

    private void check(String eventType, EventPriority priority, String playerName, UUID playerUuid, String ip, BiConsumer<String, String> disallow) {
        if (!isEnabled()) return;
        if (!eventType.equals(DiscordSRV.config().getString("Require linked account to play.Listener event"))) return;

        String requestedPriority = DiscordSRV.config().getString("Require linked account to play.Listener priority");
        EventPriority targetPriority = Arrays.stream(EventPriority.values())
                .filter(p -> p.name().equalsIgnoreCase(requestedPriority))
                .findFirst().orElse(EventPriority.LOWEST);
        if (priority != targetPriority) return;

        try {
            if (getBypassNames().contains(playerName)) {
                DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + playerName + " is on the bypass list, bypassing linking checks");
                return;
            }

            if (checkWhitelist()) {
                boolean whitelisted = Bukkit.getServer().getWhitelistedPlayers().stream().map(OfflinePlayer::getUniqueId).anyMatch(u -> u.equals(playerUuid));
                if (whitelisted) {
                    DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + playerName + " is bypassing link requirement, player is whitelisted");
                    return;
                }
            }
            boolean onlyCheckBannedPlayers = onlyCheckBannedPlayers();
            if (!checkBannedPlayers() || onlyCheckBannedPlayers) {
                boolean banned = false;
                if (Bukkit.getServer().getBannedPlayers().stream().anyMatch(p -> p.getUniqueId().equals(playerUuid))) {
                    if (!onlyCheckBannedPlayers) {
                        DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + playerName + " is banned, skipping linked check");
                        return;
                    }
                    banned = true;
                }
                if (!banned && Bukkit.getServer().getIPBans().stream().anyMatch(ip::equals)) {
                    if (!onlyCheckBannedPlayers) {
                        DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + playerName + " connecting with banned IP " + ip + ", skipping linked check");
                        return;
                    }
                    banned = true;
                }
                if (onlyCheckBannedPlayers && !banned) {
                    DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + playerName + " is bypassing link requirement because \"Only check banned players\" is enabled");
                    return;
                }
            }

            if (!DiscordSRV.isReady) {
                DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + playerName + " connecting before DiscordSRV is ready, denying login");
                disallow.accept(AsyncPlayerPreLoginEvent.Result.KICK_OTHER.name(), MessageUtil.translateLegacy(getDiscordSRVStillStartingKickMessage()));
                return;
            }

            String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordIdBypassCache(playerUuid);
            if (discordId == null) {
                Member botMember = DiscordSRV.getPlugin().getMainGuild().getSelfMember();
                String botName = botMember.getEffectiveName() + "#" + botMember.getUser().getDiscriminator();
                String code = DiscordSRV.getPlugin().getAccountLinkManager().generateCode(playerUuid);
                String inviteLink = DiscordSRV.config().getString("DiscordInviteLink");

                DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + playerName + " is NOT linked to a Discord account, denying login");
                disallow.accept(
                        AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST.name(),
                        MessageUtil.translateLegacy(DiscordSRV.config().getString("Require linked account to play.Not linked message"))
                                .replace("{BOT}", botName)
                                .replace("{CODE}", code)
                                .replace("{INVITE}", inviteLink)
                );
                return;
            }

            Dynamic mustBeInDiscordServerOption = DiscordSRV.config().dget("Require linked account to play.Must be in Discord server");
            if (mustBeInDiscordServerOption.is(Boolean.class)) {
                boolean mustBePresent = mustBeInDiscordServerOption.as(Boolean.class);
                boolean isPresent = DiscordUtil.getJda().retrieveUserById(discordId).complete().getMutualGuilds().contains(DiscordSRV.getPlugin().getMainGuild());
                if (mustBePresent && !isPresent) {
                    DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + playerName + "'s linked Discord account is NOT present, denying login");
                    disallow.accept(
                            AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST.name(),
                            MessageUtil.translateLegacy(DiscordSRV.config().getString("Require linked account to play.Messages.Not in server"))
                                    .replace("{INVITE}", DiscordSRV.config().getString("DiscordInviteLink"))
                    );
                    return;
                }
            } else {
                Set<String> targets = new HashSet<>();

                if (mustBeInDiscordServerOption.isList()) {
                    mustBeInDiscordServerOption.children().forEach(dynamic -> targets.add(dynamic.toString()));
                } else {
                    targets.add(mustBeInDiscordServerOption.convert().intoString());
                }

                for (String guildId : targets) {
                    try {
                        Guild guild = DiscordUtil.getJda().getGuildById(guildId);
                        if (guild != null) {
                            boolean inServer = guild.retrieveMemberById(discordId).complete() != null;
                            if (!inServer) {
                                DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + playerName + "'s linked Discord account is NOT present, denying login");
                                disallow.accept(
                                        AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST.name(),
                                        MessageUtil.translateLegacy(DiscordSRV.config().getString("Require linked account to play.Messages.Not in server"))
                                                .replace("{INVITE}", DiscordSRV.config().getString("DiscordInviteLink"))
                                );
                                return;
                            }
                        } else {
                            DiscordSRV.debug(Debug.REQUIRE_LINK, "Failed to get Discord server by ID " + guildId + ": bot is not in server");
                        }
                    } catch (NumberFormatException e) {
                        DiscordSRV.debug(Debug.REQUIRE_LINK, "Failed to get Discord server by ID " + guildId + ": not a parsable long");
                    }
                }
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

                    Role role = null;
                    try {
                        role = DiscordUtil.getJda().getRoleById(subRoleId);
                    } catch (Throwable ignored) {}
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
                    DiscordSRV.error("Tried to authenticate " + playerName + " but no valid subscriber role IDs are found and thats a requirement; login will be denied until this is fixed.");
                    disallow.accept(AsyncPlayerPreLoginEvent.Result.KICK_OTHER.name(), MessageUtil.translateLegacy(getFailedToFindRoleKickMessage()));
                    return;
                }

                if (getAllSubRolesRequired() ? matches < subRoleIds.size() : matches == 0) {
                    DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + playerName + " does NOT match subscriber role requirements, denying login");
                    disallow.accept(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST.name(), MessageUtil.translateLegacy(getSubscriberRoleKickMessage()));
                }
            }
        } catch (Exception exception) {
            DiscordSRV.error("Failed to check player: " + playerName, exception);
            disallow.accept(AsyncPlayerPreLoginEvent.Result.KICK_OTHER.name(), MessageUtil.translateLegacy(getUnknownFailureKickMessage()));
        }
    }

    public void noticePlayerUnlink(Player player) {
        if (!isEnabled()) return;
        if (getBypassNames().contains(player.getName())) return;
        if (checkWhitelist()) {
            boolean whitelisted = Bukkit.getServer().getWhitelistedPlayers().stream().map(OfflinePlayer::getUniqueId).anyMatch(u -> u.equals(player.getUniqueId()));
            if (whitelisted) {
                DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + player.getName() + " is bypassing link requirement, player is whitelisted");
                return;
            }
        }
        String ip = player.getAddress().getAddress().getHostAddress();
        if (onlyCheckBannedPlayers() && !Bukkit.getServer().getBannedPlayers().stream().anyMatch(p -> p.getUniqueId().equals(player.getUniqueId())) && !Bukkit.getServer().getIPBans().stream().anyMatch(ip::equals)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "Player " + player.getName() + " is bypassing link requirement because \"Only check banned players\" is enabled");
            return;
        }

        DiscordSRV.info("Kicking player " + player.getName() + " for unlinking their accounts");
        Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), () -> player.kickPlayer(MessageUtil.translateLegacy(getUnlinkedKickMessage())));
    }

    private boolean checkWhitelist() {
        return DiscordSRV.config().getBoolean("Require linked account to play.Whitelisted players bypass check");
    }
    private boolean checkBannedPlayers() {
        return DiscordSRV.config().getBoolean("Require linked account to play.Check banned players");
    }
    private boolean onlyCheckBannedPlayers() {
        return DiscordSRV.config().getBoolean("Require linked account to play.Only check banned players");
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
        if (!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "PlayerLoginEvent event result for " + event.getName() + " = " + event.getLoginResult() + ", skipping");
            return;
        }
        check(event.getClass().getSimpleName(), EventPriority.LOWEST, event.getName(), event.getUniqueId(), event.getAddress().getHostAddress(), (result, message) -> event.disallow(AsyncPlayerPreLoginEvent.Result.valueOf(result), message));
    }
    @EventHandler(priority = EventPriority.LOW)
    public void onEventLow(AsyncPlayerPreLoginEvent event) {
        if (!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "PlayerLoginEvent event result for " + event.getName() + " = " + event.getLoginResult() + ", skipping");
            return;
        }
        check(event.getClass().getSimpleName(), EventPriority.LOW, event.getName(), event.getUniqueId(), event.getAddress().getHostAddress(), (result, message) -> event.disallow(AsyncPlayerPreLoginEvent.Result.valueOf(result), message));
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEventNormal(AsyncPlayerPreLoginEvent event) {
        if (!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "PlayerLoginEvent event result for " + event.getName() + " = " + event.getLoginResult() + ", skipping");
            return;
        }
        check(event.getClass().getSimpleName(), EventPriority.NORMAL, event.getName(), event.getUniqueId(), event.getAddress().getHostAddress(), (result, message) -> event.disallow(AsyncPlayerPreLoginEvent.Result.valueOf(result), message));
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onEventHigh(AsyncPlayerPreLoginEvent event) {
        if (!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "PlayerLoginEvent event result for " + event.getName() + " = " + event.getLoginResult() + ", skipping");
            return;
        }
        check(event.getClass().getSimpleName(), EventPriority.HIGH, event.getName(), event.getUniqueId(), event.getAddress().getHostAddress(), (result, message) -> event.disallow(AsyncPlayerPreLoginEvent.Result.valueOf(result), message));
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEventHighest(AsyncPlayerPreLoginEvent event) {
        if (!event.getLoginResult().equals(AsyncPlayerPreLoginEvent.Result.ALLOWED)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "PlayerLoginEvent event result for " + event.getName() + " = " + event.getLoginResult() + ", skipping");
            return;
        }
        check(event.getClass().getSimpleName(), EventPriority.HIGHEST, event.getName(), event.getUniqueId(), event.getAddress().getHostAddress(), (result, message) -> event.disallow(AsyncPlayerPreLoginEvent.Result.valueOf(result), message));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEventLowest(PlayerLoginEvent event) {
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "PlayerLoginEvent event result for " + event.getPlayer().getName() + " = " + event.getResult() + ", skipping");
            return;
        }
        check(event.getClass().getSimpleName(), EventPriority.LOWEST, event.getPlayer().getName(), event.getPlayer().getUniqueId(), event.getAddress().getHostAddress(), (result, message) -> event.disallow(PlayerLoginEvent.Result.valueOf(result), message));
    }
    @EventHandler(priority = EventPriority.LOW)
    public void onEventLow(PlayerLoginEvent event) {
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "PlayerLoginEvent event result for " + event.getPlayer().getName() + " = " + event.getResult() + ", skipping");
            return;
        }
        check(event.getClass().getSimpleName(), EventPriority.LOW, event.getPlayer().getName(), event.getPlayer().getUniqueId(), event.getAddress().getHostAddress(), (result, message) -> event.disallow(PlayerLoginEvent.Result.valueOf(result), message));
    }
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEventNormal(PlayerLoginEvent event) {
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "PlayerLoginEvent event result for " + event.getPlayer().getName() + " = " + event.getResult() + ", skipping");
            return;
        }
        check(event.getClass().getSimpleName(), EventPriority.NORMAL, event.getPlayer().getName(), event.getPlayer().getUniqueId(), event.getAddress().getHostAddress(), (result, message) -> event.disallow(PlayerLoginEvent.Result.valueOf(result), message));
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onEventHigh(PlayerLoginEvent event) {
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "PlayerLoginEvent event result for " + event.getPlayer().getName() + " = " + event.getResult() + ", skipping");
            return;
        }
        check(event.getClass().getSimpleName(), EventPriority.HIGH, event.getPlayer().getName(), event.getPlayer().getUniqueId(), event.getAddress().getHostAddress(), (result, message) -> event.disallow(PlayerLoginEvent.Result.valueOf(result), message));
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEventHighest(PlayerLoginEvent event) {
        if (!event.getResult().equals(PlayerLoginEvent.Result.ALLOWED)) {
            DiscordSRV.debug(Debug.REQUIRE_LINK, "PlayerLoginEvent event result for " + event.getPlayer().getName() + " = " + event.getResult() + ", skipping");
            return;
        }
        check(event.getClass().getSimpleName(), EventPriority.HIGHEST, event.getPlayer().getName(), event.getPlayer().getUniqueId(), event.getAddress().getHostAddress(), (result, message) -> event.disallow(PlayerLoginEvent.Result.valueOf(result), message));
    }

}
