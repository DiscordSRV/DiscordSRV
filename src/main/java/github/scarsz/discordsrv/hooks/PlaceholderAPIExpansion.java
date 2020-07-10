package github.scarsz.discordsrv.hooks;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (!DiscordSRV.isReady) return "...";

        Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
        if (mainGuild == null) return "";

        Supplier<List<String>> membersOnline = () -> mainGuild.getMembers().stream()
                .filter(member -> member.getOnlineStatus() != OnlineStatus.OFFLINE)
                .map(member -> member.getUser().getId())
                .collect(Collectors.toList());
        Supplier<Set<String>> linkedAccounts = () -> DiscordSRV.getPlugin().getAccountLinkManager().getLinkedAccounts().keySet();

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
                return String.valueOf(membersOnline.get().size());
            case "guild_members_total":
                return String.valueOf(mainGuild.getMembers().size());
            case "linked_online":
                List<String> onlineMembers = membersOnline.get();
                return String.valueOf(linkedAccounts.get().stream().filter(onlineMembers::contains).count());
            case "linked_total":
                return String.valueOf(linkedAccounts.get().size());
        }

        if (player == null) return "";

        String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
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

        List<Role> selectedRoles = getRoles(member);
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

    /**
     * Get roles from a member, filtered based on
     * Source: https://github.com/DiscordSRV/DiscordSRV/blob/6b8de4afb3bfecf9c63275d381c75b103e5543f3/src/main/java/github/scarsz/discordsrv/listeners/DiscordChatListener.java#L110-L122
     *
     * @param member The member to get the roles from
     * @return filtered list of roles
     */
    private List<Role> getRoles(Member member) {
        List<Role> selectedRoles;
        List<String> discordRolesSelection = DiscordSRV.config().getStringList("DiscordChatChannelRolesSelection");
        // if we have a whitelist in the config
        if (DiscordSRV.config().getBoolean("DiscordChatChannelRolesSelectionAsWhitelist")) {
            selectedRoles = member.getRoles().stream()
                    .filter(role -> discordRolesSelection.contains(DiscordUtil.getRoleName(role)))
                    .collect(Collectors.toList());
        } else { // if we have a blacklist in the settings
            selectedRoles = member.getRoles().stream()
                    .filter(role -> !discordRolesSelection.contains(DiscordUtil.getRoleName(role)))
                    .collect(Collectors.toList());
        }
        selectedRoles.removeIf(role -> role.getName().length() < 1);

        return selectedRoles;
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
