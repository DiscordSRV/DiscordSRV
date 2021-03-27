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

package github.scarsz.discordsrv.hooks;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.managers.AccountLinkManager;
import github.scarsz.discordsrv.objects.managers.link.JdbcAccountLinkManager;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    private long lastIssue = -1;

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String identifier) {
        if (!DiscordSRV.isReady) return "...";

        Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
        if (mainGuild == null) return "";

        Set<Member> onlineMembers = mainGuild.getMemberCache().stream()
                .filter(member -> member.getOnlineStatus() != OnlineStatus.OFFLINE)
                .collect(Collectors.toSet());
        Set<String> onlineMemberIds = onlineMembers.stream().map(Member::getId).collect(Collectors.toSet());
        AccountLinkManager accountLinkManager = DiscordSRV.getPlugin().getAccountLinkManager();
        Supplier<Set<String>> linkedAccounts = () -> {
            if (accountLinkManager instanceof JdbcAccountLinkManager && Bukkit.isPrimaryThread()) {
                // not permitted
                long currentTime = System.currentTimeMillis();
                if (lastIssue + TimeUnit.SECONDS.toMillis(10) < currentTime) {
                    DiscordSRV.warning("The %discordsrv_linked_online% placeholder was requested via PlaceholderAPI on the main thread while JDBC is enabled, this is unsupported");
                    lastIssue = currentTime;
                }
                return Collections.emptySet();
            }
            return accountLinkManager.getLinkedAccounts().keySet();
        };

        switch (identifier) {
            case "guild_id":
                return mainGuild.getId();
            case "guild_name":
                return mainGuild.getName();
            case "guild_icon_id":
                return orEmptyString(mainGuild.getIconId());
            case "guild_icon_url":
                return orEmptyString(mainGuild.getIconUrl());
            case "guild_splash_id":
                return orEmptyString(mainGuild.getSplashId());
            case "guild_splash_url":
                return orEmptyString(mainGuild.getSplashUrl());
            case "guild_owner_effective_name":
                return applyOrEmptyString(mainGuild.getOwner(), Member::getEffectiveName);
            case "guild_owner_nickname":
                return applyOrEmptyString(mainGuild.getOwner(), Member::getNickname);
            case "guild_owner_game_name":
                return applyOrEmptyString(mainGuild.getOwner(), member -> member.getActivities().stream().findFirst().map(Activity::getName).orElse(""));
            case "guild_owner_game_url":
                return applyOrEmptyString(mainGuild.getOwner(), member -> member.getActivities().stream().findFirst().map(Activity::getUrl).orElse(""));
            case "guild_bot_effective_name":
                return mainGuild.getSelfMember().getEffectiveName();
            case "guild_bot_nickname":
                return orEmptyString(mainGuild.getSelfMember().getNickname());
            case "guild_bot_game_name":
                return applyOrEmptyString(mainGuild.getSelfMember(), member -> member.getActivities().stream().findFirst().map(Activity::getName).orElse(""));
            case "guild_bot_game_url":
                return applyOrEmptyString(mainGuild.getSelfMember(), member -> member.getActivities().stream().findFirst().map(Activity::getUrl).orElse(""));
            case "guild_members_online":
                return String.valueOf(onlineMembers.size());
            case "guild_members_total":
                return String.valueOf(mainGuild.getMembers().size());
            case "linked_online":
                return String.valueOf(linkedAccounts.get().stream().filter(onlineMemberIds::contains).count());
            case "linked_total":
                return String.valueOf(accountLinkManager.getLinkedAccountCount());
        }

        if (player == null) return "";

        String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordIdBypassCache(player.getUniqueId());
        switch (identifier) {
            case "user_id":
                return orEmptyString(userId);
            case "user_islinked":
                return getBoolean(userId != null);
        }

        User user = DiscordUtil.getUserById(userId);
        if (user == null) return "";

        switch (identifier) {
            case "user_name":
                return user.getName();
            case "user_tag":
                return user.getAsTag();
        }

        Member member = mainGuild.getMember(user);
        if (member == null) return "";

        switch (identifier) {
            case "user_effective_name":
                return member.getEffectiveName();
            case "user_nickname":
                return orEmptyString(member.getNickname());
            case "user_online_status":
                return member.getOnlineStatus().getKey();
            case "user_game_name":
                return member.getActivities().stream().findFirst().map(Activity::getName).orElse("");
            case "user_game_url":
                return member.getActivities().stream().findFirst().map(Activity::getUrl).orElse("");
        }

        if (member.getRoles().isEmpty()) return "";

        List<Role> selectedRoles = DiscordSRV.getPlugin().getSelectedRoles(member);
        if (selectedRoles.isEmpty()) return "";

        Role topRole = DiscordUtil.getTopRole(member);
        if (topRole != null) {
            switch (identifier) {
                case "user_top_role_id":
                    return topRole.getId();
                case "user_top_role_name":
                    return topRole.getName();
                case "user_top_role_color_hex":
                    return applyOrEmptyString(topRole.getColor(), this::getHex);
                case "user_top_role_color_code":
                    return DiscordUtil.convertRoleToMinecraftColor(topRole);
            }
        }

        return null;
    }

    private String getHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private <T> String applyOrEmptyString(T input, Function<T, String> function) {
        if (input == null) return "";
        String output = function.apply(input);
        return orEmptyString(output);
    }

    private String orEmptyString(String input) {
        return StringUtils.isNotBlank(input) ? input : "";
    }

    private String getBoolean(boolean input) {
        return input ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
    }

    @Override
    public String getIdentifier() {
        return "discordsrv";
    }

    @Override
    public String getAuthor() {
        return DiscordSRV.getPlugin().getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return DiscordSRV.getPlugin().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

}
