/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package github.scarsz.discordsrv.util;

import com.google.common.collect.ImmutableMap;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageSentEvent;
import github.scarsz.discordsrv.api.events.DiscordPrivateMessageSentEvent;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;

import java.awt.Color;
import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiscordUtil {

    /**
     * Get the current JDA object that DiscordSRV is utilizing
     * @return JDA
     */
    public static JDA getJda() {
        return DiscordSRV.getPlugin().getJda();
    }

    /**
     * Get the given Role's name
     * @param role Role to get the name of
     * @return The name of the Role; if the Role is null, a blank string.
     */
    public static String getRoleName(Role role) {
        return role == null ? "" : role.getName();
    }

    /**
     * Get the top hierarchical Role of the Member
     * @param member Member to get the top role of
     * @return The top hierarchical Role
     */
    public static Role getTopRole(Member member) {
        return member.getRoles().size() != 0 ? member.getRoles().get(0) : null;
    }

    /**
     * Get the top hierarchical Role of the Member that contains a custom color
     * @param member Member to get the top custom-colored role of
     * @return The top hierarchical Role with a custom color
     */
    public static Role getTopRoleWithCustomColor(Member member) {
        for (Role role : member.getRoles()) if (role.getColor() != null) return role;
        return null;
    }

    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("(<@!?([0-9]{16,20})>)");
    private static final Pattern CHANNEL_MENTION_PATTERN = Pattern.compile("(<#([0-9]{16,20})>)");
    private static final Pattern ROLE_MENTION_PATTERN = Pattern.compile("(<@&([0-9]{16,20})>)");
    private static final Pattern EMOTE_MENTION_PATTERN = Pattern.compile("(<a?:([a-zA-Z]{2,32}):[0-9]{16,20}>)");

    /**
     * Converts Discord-compatible <@12345742934270> mentions to human readable @mentions
     * @param message the message
     * @return the converted message
     */
    public static String convertMentionsToNames(String message) {
        Matcher userMatcher = USER_MENTION_PATTERN.matcher(message);
        while (userMatcher.find()) {
            String mention = userMatcher.group(1);
            String userId = userMatcher.group(2);
            User user = getUserById(userId);
            message = message.replace(mention, user != null ? "@" + user.getName() : mention);
        }

        Matcher channelMatcher = CHANNEL_MENTION_PATTERN.matcher(message);
        while (channelMatcher.find()) {
            String mention = channelMatcher.group(1);
            String channelId = channelMatcher.group(2);
            TextChannel channel = getTextChannelById(channelId);
            message = message.replace(mention, channel != null ? "#" + channel.getName() : mention);
        }

        Matcher roleMatcher = ROLE_MENTION_PATTERN.matcher(message);
        while (roleMatcher.find()) {
            String mention = roleMatcher.group(1);
            String roleId = roleMatcher.group(2);
            Role role = getRole(roleId);
            message = message.replace(mention, role != null ? "@" + role.getName() : mention);
        }

        Matcher emoteMatcher = EMOTE_MENTION_PATTERN.matcher(message);
        while (emoteMatcher.find()) {
            message = message.replace(emoteMatcher.group(1), ":" + emoteMatcher.group(2) + ":");
        }

        return message;
    }

    /**
     * Convert @mentions into Discord-compatible <@012345678901234567890> mentions
     * @param message Message to convert
     * @param guild Guild to find names to convert
     * @return Contents of the given message with names converted to mentions
     */
    public static String convertMentionsFromNames(String message, Guild guild) {
        if (!message.contains("@")) return message;

        for (Role role : guild.getRoles()) {
            Pattern pattern = mentionPatternCache.computeIfAbsent(
                    role.getId(),
                    mentionable -> Pattern.compile(
                            Pattern.quote("@" + role.getName()),
                            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                    )
            );
            message = pattern.matcher(message).replaceAll(role.getAsMention());
        }

        for (Member member : guild.getMembers()) {
            Pattern pattern = mentionPatternCache.computeIfAbsent(
                    member.getId(),
                    mentionable -> Pattern.compile(
                            Pattern.quote("@" + member.getEffectiveName()),
                            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                    )
            );
            message = pattern.matcher(message).replaceAll(member.getAsMention());
        }

        return message;
    }
    private static Map<String, Pattern> mentionPatternCache = new HashMap<>();
    static {
        // event listener to clear the cache of invalid patterns because of name changes
        if (DiscordUtil.getJda() != null) {
            DiscordUtil.getJda().addEventListener(new ListenerAdapter() {
                @Override
                public void onUserUpdateName(UserUpdateNameEvent event) {
                    mentionPatternCache.remove(event.getUser().getId());
                }
                @Override
                public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
                    mentionPatternCache.remove(event.getMember().getId());
                }
                @Override
                public void onRoleUpdateName(RoleUpdateNameEvent event) {
                    mentionPatternCache.remove(event.getRole().getId());
                }
            });
        }
    }

    /**
     * Return the given String with Markdown escaped. Useful for sending things to Discord.
     * @param text String to escape markdown in
     * @return String with markdown escaped
     */
    public static String escapeMarkdown(String text) {
        return text == null ? "" : text.replace("_", "\\_").replace("*", "\\*").replace("~", "\\~");
    }

    /**
     * regex-powered stripping pattern, see https://regex101.com/r/IzirAR/2 for explanation
     */
    private static final Pattern stripPattern = Pattern.compile("(?<!@)[&§](?i)[0-9a-fklmnor]");
    private static final Pattern stripSectionOnlyPattern = Pattern.compile("(?<!@)§(?i)[0-9a-fklmnor]");

    /**
     * regex-powered aggressive stripping pattern, see https://regex101.com/r/mW8OlT for explanation
     */
    private static final Pattern aggressiveStripPattern = Pattern.compile("\\[m|\\[([0-9]{1,2}[;m]?){3}|\u001B+");

    /**
     * Strip the given String of Minecraft coloring. Useful for sending things to Discord.
     * @param text the given String to strip colors from
     * @return the given String with coloring stripped
     */
    public static String strip(String text) {
        if (StringUtils.isBlank(text)) {
            DiscordSRV.debug("Tried stripping blank message");
            return "";
        }

//        TODO: revisit this
//        // Replace invisible control characters and unused code points
//        StringBuilder newString = new StringBuilder(newText.length());
//        for (int offset = 0; offset < newText.length();) {
//            if (newText.substring(offset, offset + 1).equals("\n")) {
//                newString.append("\n");
//                continue;
//            }
//
//            int codePoint = newText.codePointAt(offset);
//            offset += Character.charCount(codePoint);
//
//            switch (Character.getType(codePoint)) {
//                case Character.CONTROL:     // \p{Cc}
//                case Character.FORMAT:      // \p{Cf}
//                case Character.PRIVATE_USE: // \p{Co}
//                case Character.SURROGATE:   // \p{Cs}
//                case Character.UNASSIGNED:  // \p{Cn}
//                    break;
//                default:
//                    newString.append(Character.toChars(codePoint));
//                    break;
//            }
//        }
//
//        return newString.toString();

        return stripPattern.matcher(text).replaceAll("");
    }

    public static String stripSectionOnly(String text) {
        return stripSectionOnlyPattern.matcher(text).replaceAll("");
    }

    public static String aggressiveStrip(String text) {
        if (StringUtils.isBlank(text)) {
            DiscordSRV.debug("Tried aggressively stripping blank message");
            return null;
        }

        return aggressiveStripPattern.matcher(text).replaceAll("");
    }

    /**
     * Send the given String message to the given TextChannel
     * @param channel Channel to send the message to
     * @param message Message to send to the channel
     */
    public static void sendMessage(TextChannel channel, String message) {
        sendMessage(channel, message, 0, true);
    }
    /**
     * Send the given String message to the given TextChannel that will expire in x milliseconds
     * @param channel the TextChannel to send the message to
     * @param message the message to send to the TextChannel
     * @param expiration milliseconds until expiration of message. if this is 0, the message will not expire
     */
    public static void sendMessage(TextChannel channel, String message, int expiration, boolean editMessage) {
        if (channel == null) {
            DiscordSRV.debug("Tried sending a message to a null channel");
            return;
        }

        if (getJda() == null) {
            DiscordSRV.debug("Tried sending a message using a null JDA instance");
            return;
        }

        if (message == null) {
            DiscordSRV.debug("Tried sending a null message to " + channel);
            return;
        }

        if (StringUtils.isBlank(message)) {
            DiscordSRV.debug("Tried sending a blank message to " + channel);
            return;
        }

        message = DiscordUtil.strip(message);
        if (editMessage) {
            message = DiscordUtil.cutPhrases(message);
        }

        String overflow = null;
        int maxLength = Message.MAX_CONTENT_LENGTH;
        if (message.length() > maxLength) {
            DiscordSRV.debug("Tried sending message with length of " + message.length() + " (" + (message.length() - maxLength) + " over limit)");
            overflow = message.substring(maxLength);
            message = message.substring(0, maxLength);
        }

        queueMessage(channel, message, m -> {
            if (expiration > 0) {
                try { Thread.sleep(expiration); } catch (InterruptedException e) { e.printStackTrace(); }
                deleteMessage(m);
            }
        });
        if (overflow != null) sendMessage(channel, overflow, expiration, editMessage);
    }

    public static String cutPhrases(String message) {
        if (DiscordSRV.config().getStringList("DiscordChatChannelCutPhrases").size() > 0) {
            int changes;
            do {
                changes = 0;
                String before = message;
                for (String phrase : DiscordSRV.config().getStringList("DiscordChatChannelCutPhrases")) {
                    // case insensitive String#replace(phrase, "")
                    message = message.replaceAll("(?i)" + Pattern.quote(phrase), "");
                    changes += before.length() - message.length();
                }
            } while (changes > 0); // keep cutting until there are no changes
        }
        return message;
    }

    /**
     * Check if the bot has the given permission in the given channel
     * @param channel Channel to check for the permission in
     * @param permission Permission to be checked for
     * @return true if the permission is obtained, false otherwise
     */
    public static boolean checkPermission(GuildChannel channel, Permission permission) {
        return checkPermission(channel, getJda().getSelfUser(), permission);
    }
    /**
     * Check if the bot has the given guild permission
     * @param guild Guild to check for the permission in
     * @param permission Permission to be checked for
     * @return true if the permission is obtained, false otherwise
     */
    public static boolean checkPermission(Guild guild, Permission permission) {
        return guild != null && guild.getSelfMember().hasPermission(permission);
    }
    /**
     * Check if the given user has the given permission in the given channel
     * @param channel Channel to check for the permission in
     * @param user User to check permissions for
     * @param permission Permission to be checked for
     * @return true if the permission is obtained, false otherwise
     */
    public static boolean checkPermission(GuildChannel channel, User user, Permission permission) {
        if (channel == null) return false;
        Member member = channel.getGuild().getMember(user);
        if (member == null) return false;
        return member.hasPermission(channel, permission);
    }

    /**
     * Send the given message to the given channel, blocking the thread's execution until it's successfully sent then returning it
     * @param channel The channel to send the message to
     * @param message The message to send to the channel
     * @return The sent message
     */
    public static Message sendMessageBlocking(TextChannel channel, String message) {
        if (message == null || StringUtils.isBlank(message)) {
            DiscordSRV.debug("Tried sending a null or blank message");
            return null;
        }

        if (channel == null) {
            DiscordSRV.debug("Tried sending a message to a null channel");
            return null;
        }

        message = translateEmotes(message, channel.getGuild());

        return sendMessageBlocking(channel, new MessageBuilder().append(message).build());
    }
    /**
     * Send the given message to the given channel, blocking the thread's execution until it's successfully sent then returning it
     * @param channel The channel to send the message to
     * @param message The message to send to the channel
     * @return The sent message
     */
    public static Message sendMessageBlocking(TextChannel channel, Message message) {
        if (getJda() == null) {
            DiscordSRV.debug("Tried sending a message when JDA was null");
            return null;
        }

        if (channel == null) {
            DiscordSRV.debug("Tried sending a message to a null channel");
            return null;
        }

        if (message == null || StringUtils.isBlank(message.getContentRaw())) {
            DiscordSRV.debug("Tried sending a null or blank message");
            return null;
        }

        Message sentMessage;
        try {
            sentMessage = channel.sendMessage(message).complete();
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not send message in channel " + channel + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not send message in channel " + channel + " because \"" + e.getMessage() + "\"");
            }
            return null;
        }
        DiscordSRV.api.callEvent(new DiscordGuildMessageSentEvent(getJda(), sentMessage));

        return sentMessage;
    }

    /**
     * Send the given message to the given channel
     * @param channel The channel to send the message to
     * @param message The message to send to the channel
     */
    public static void queueMessage(TextChannel channel, String message) {
        if (channel == null) {
            DiscordSRV.debug("Tried sending a message to a null channel");
            return;
        }

        message = translateEmotes(message, channel.getGuild());
        queueMessage(channel, new MessageBuilder().append(message).build());
    }
    /**
     * Send the given message to the given channel
     * @param channel The channel to send the message to
     * @param message The message to send to the channel
     */
    public static void queueMessage(TextChannel channel, Message message) {
        queueMessage(channel, message, null);
    }
    /**
     * Send the given message to the given channel, optionally doing something with the message via the given consumer
     * @param channel The channel to send the message to
     * @param message The message to send to the channel
     * @param consumer The consumer to handle the message
     */
    public static void queueMessage(TextChannel channel, String message, Consumer<Message> consumer) {
        message = translateEmotes(message, channel.getGuild());
        queueMessage(channel, new MessageBuilder().append(message).build(), consumer);
    }
    /**
     * Send the given message to the given channel, optionally doing something with the message via the given consumer
     * @param channel The channel to send the message to
     * @param message The message to send to the channel
     * @param consumer The consumer to handle the message
     */
    public static void queueMessage(TextChannel channel, Message message, Consumer<Message> consumer) {
        if (channel == null) {
            DiscordSRV.debug("Tried sending a message to a null channel");
            return;
        }

        try {
            channel.sendMessage(message).queue(sentMessage -> {
                DiscordSRV.api.callEvent(new DiscordGuildMessageSentEvent(getJda(), sentMessage));
                if (consumer != null) consumer.accept(sentMessage);
            }, throwable -> DiscordSRV.error("Failed to send message to channel " + channel + ": " + throwable.getMessage()));
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not send message in channel " + channel + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not send message in channel " + channel + " because \"" + e.getMessage() + "\"");
            }
        } catch (IllegalStateException e) {
            DiscordSRV.error("Could not send message to channel " + channel + ": " + e.getMessage());
        }
    }

    /**
     * Set the topic message of the given channel
     * @param channel The channel to set the topic of
     * @param topic The new topic to be set
     */
    public static void setTextChannelTopic(TextChannel channel, String topic) {
        if (channel == null) {
            DiscordSRV.debug("Attempted to set status of null channel");
            return;
        }

        try {
            channel.getManager().setTopic(topic).queue();
        } catch (Exception e) {
            if (e instanceof PermissionException) {
                PermissionException pe = (PermissionException) e;
                if (pe.getPermission() != Permission.UNKNOWN) {
                    DiscordSRV.warning("Could not set topic of channel " + channel + " because the bot does not have the \"" + pe.getPermission().getName() + "\" permission");
                }
            } else {
                DiscordSRV.warning("Could not set topic of channel " + channel + " because \"" + e.getMessage() + "\"");
            }
        }
    }

    /**
     * Set the game status of the bot
     * @param gameStatus The game status to be set
     */
    public static void setGameStatus(String gameStatus) {
        if (getJda() == null) {
            DiscordSRV.debug("Attempted to set game status using null JDA");
            return;
        }
        if (StringUtils.isBlank(gameStatus)) {
            DiscordSRV.debug("Attempted setting game status to a null or empty string");
            return;
        }

        // set PAPI placeholders
        if (PluginUtil.pluginHookIsEnabled("placeholderapi")) gameStatus = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(null, gameStatus);

        getJda().getPresence().setActivity(Activity.playing(gameStatus));
    }

    /**
     * Delete the given message, given the bot has permission to
     * @param message The message to delete
     */
    public static void deleteMessage(Message message) {
        if (message.isFromType(ChannelType.PRIVATE)) return;

        try {
            message.delete().queue();
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not delete message in channel " + message.getTextChannel() + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not delete message in channel " + message.getTextChannel() + " because \"" + e.getMessage() + "\"");
            }
        }
    }

    /**
     * Open the private channel for the given user and send them the given message
     * @param user User to send the message to
     * @param message Message to send to the user
     */
    public static void privateMessage(User user, String message) {
        user.openPrivateChannel().queue(privateChannel ->
                privateChannel.sendMessage(message).queue(sentMessage ->
                        DiscordSRV.api.callEvent(new DiscordPrivateMessageSentEvent(getJda(), sentMessage))
                )
        );
    }

    public static boolean memberHasRole(Member member, Set<String> rolesToCheck) {
        Set<String> rolesLowercase = rolesToCheck.stream().map(String::toLowerCase).collect(Collectors.toSet());
        return member.getRoles().stream().anyMatch(role -> rolesLowercase.contains(role.getName().toLowerCase()));
    }

    private static final Color discordDefaultColor = new Color(153, 170, 181, 1);
    private static final Map<Color, ChatColor> minecraftColors = ImmutableMap.copyOf(new HashMap<Color, ChatColor>() {{
        put(new Color(0, 0, 0), ChatColor.BLACK);
        put(new Color(0, 0, 170), ChatColor.DARK_BLUE);
        put(new Color(0, 170, 0), ChatColor.DARK_GREEN);
        put(new Color(0, 170, 170), ChatColor.DARK_AQUA);
        put(new Color(170, 0, 0), ChatColor.DARK_RED);
        put(new Color(170, 0, 170), ChatColor.DARK_PURPLE);
        put(new Color(255, 170, 0), ChatColor.GOLD);
        put(new Color(170, 170, 170), ChatColor.GRAY);
        put(new Color(85, 85, 85), ChatColor.DARK_GRAY);
        put(new Color(85, 85, 255), ChatColor.BLUE);
        put(new Color(85, 255, 85), ChatColor.GREEN);
        put(new Color(85, 255, 255), ChatColor.AQUA);
        put(new Color(255, 85, 85), ChatColor.RED);
        put(new Color(255, 85, 255), ChatColor.LIGHT_PURPLE);
        put(new Color(255, 255, 85), ChatColor.YELLOW);
        put(new Color(255, 255, 255), ChatColor.WHITE);
    }});

    private static int colorDistance(Color color1, Color color2) {
        return (int) Math.sqrt((color1.getRed() - color2.getRed()) * (color1.getRed() - color2.getRed())
                + (color1.getGreen() - color2.getGreen()) * (color1.getGreen() - color2.getGreen())
                + (color1.getBlue() - color2.getBlue()) * (color1.getBlue() - color2.getBlue()));
    }

    /**
     * Get the Minecraft-equivalent of the given Role for use with having corresponding colors
     * @param role The Role to look up
     * @return A String representing the Role's color in hex
     */
    public static String convertRoleToMinecraftColor(Role role) {
        if (role == null) {
            DiscordSRV.debug("Attempted to look up color for null role");
            return "";
        }

        Color color = role.getColor() != null ? role.getColor() : discordDefaultColor;
        String hex = Integer.toHexString(color.getRGB()).toUpperCase();
        if (hex.length() == 8) hex = hex.substring(2);
        String translatedColor = DiscordSRV.getPlugin().getColors().get(hex);

        if (translatedColor == null) {
            if (DiscordSRV.config().getBoolean("Experiment_Automatic_Color_Translations")) {
                DiscordSRV.debug("Looking up the color for role " + role + " (" + hex + ") with automatic translation");

                ChatColor determinedColor = minecraftColors.entrySet().stream()
                        .min(Comparator.comparingInt(entry -> colorDistance(color, entry.getKey())))
                        .map(Map.Entry::getValue)
                        .orElseThrow(() -> new RuntimeException("This should not be possible:tm:"));

                DiscordSRV.debug("Color for " + role + " determined to: " + determinedColor.name());
                translatedColor = determinedColor.toString();
            } else {
                DiscordSRV.debug("Attempted to lookup translated color " + hex + " for role " + role + " but no definition was found (and automatic translation was disabled)");
                translatedColor = "";
            }
        }

        return translatedColor;
    }

    /**
     * Get a formatted String representing a list of roles, delimited by DiscordToMinecraftAllRolesSeparator
     * @param roles The list of roles to format
     * @return The formatted String representing the list of roles
     */
    public static String getFormattedRoles(List<Role> roles) {
        return String.join(LangUtil.Message.CHAT_TO_MINECRAFT_ALL_ROLES_SEPARATOR.toString(), roles.stream()
                .map(DiscordUtil::getRoleName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList()));
    }

    public static void setAvatar(File avatar) throws RuntimeException {
        try {
            getJda().getSelfUser().getManager().setAvatar(Icon.from(avatar)).queue();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void setAvatarBlocking(File avatar) throws RuntimeException {
        try {
            getJda().getSelfUser().getManager().setAvatar(Icon.from(avatar)).complete();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void modifyRolesOfMember(Member member, Set<Role> rolesToAdd, Set<Role> rolesToRemove) {
        rolesToAdd = rolesToAdd.stream()
                .filter(role -> !role.isManaged())
                .filter(role -> !role.getGuild().getPublicRole().getId().equals(role.getId()))
                .filter(role -> !member.getRoles().contains(role))
                .collect(Collectors.toSet());
        Set<Role> nonInteractableRolesToAdd = rolesToAdd.stream().filter(role -> !member.getGuild().getSelfMember().canInteract(role)).collect(Collectors.toSet());
        rolesToAdd.removeAll(nonInteractableRolesToAdd);
        nonInteractableRolesToAdd.forEach(role -> DiscordSRV.warning("Failed to add role \"" + role.getName() + "\" to \"" + member.getEffectiveName() + "\" because the bot's highest role is lower than the target role and thus can't interact with it"));

        rolesToRemove = rolesToRemove.stream()
                .filter(role -> !role.isManaged())
                .filter(role -> !role.getGuild().getPublicRole().getId().equals(role.getId()))
                .filter(role -> member.getRoles().contains(role))
                .collect(Collectors.toSet());
        Set<Role> nonInteractableRolesToRemove = rolesToRemove.stream().filter(role -> !member.getGuild().getSelfMember().canInteract(role)).collect(Collectors.toSet());
        rolesToRemove.removeAll(nonInteractableRolesToRemove);
        nonInteractableRolesToRemove.forEach(role -> DiscordSRV.warning("Failed to remove role \"" + role.getName() + "\" from \"" + member.getEffectiveName() + "\" because the bot's highest role is lower than the target role and thus can't interact with it"));

        member.getGuild().modifyMemberRoles(member, rolesToAdd, rolesToRemove).queue();
    }

    public static void addRoleToMember(Member member, Role role) {
        if (member == null) {
            DiscordSRV.debug("Can't add role to null member");
            return;
        }

        try {
            member.getGuild().addRoleToMember(member, role).queue();
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not add " + member + " to role " + role + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not add " + member + " to role " + role + " because \"" + e.getMessage() + "\"");
            }
        }
    }

    public static void addRolesToMember(Member member, Role... roles) {
        if (member == null) {
            DiscordSRV.debug("Can't add roles to null member");
            return;
        }

        List<Role> rolesToAdd = Arrays.stream(roles)
                .filter(role -> !role.isManaged())
                .filter(role -> !role.getGuild().getPublicRole().getId().equals(role.getId()))
                .collect(Collectors.toList());

        try {
            member.getGuild().modifyMemberRoles(member, rolesToAdd, Collections.emptySet()).queue();
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not add " + member + " to role(s) " + rolesToAdd + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not add " + member + " to role(s) " + rolesToAdd + " because \"" + e.getMessage() + "\"");
            }
        }
    }

    public static void addRolesToMember(Member member, Set<Role> rolesToAdd) {
        addRolesToMember(member, rolesToAdd.toArray(new Role[0]));
    }

    public static void removeRolesFromMember(Member member, Role... roles) {
        if (member == null) {
            DiscordSRV.debug("Can't remove roles from null member");
            return;
        }

        List<Role> rolesToRemove = Arrays.stream(roles)
                .filter(role -> !role.isManaged())
                .filter(role -> !role.getGuild().getPublicRole().getId().equals(role.getId()))
                .collect(Collectors.toList());

        try {
            member.getGuild().modifyMemberRoles(member, Collections.emptySet(), rolesToRemove).queue();
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not demote " + member + " from role(s) " + rolesToRemove + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not demote " + member + " from role(s) " + rolesToRemove + " because \"" + e.getMessage() + "\"");
            }
        }
    }
    public static void removeRolesFromMember(Member member, Set<Role> rolesToRemove) {
        removeRolesFromMember(member, rolesToRemove.toArray(new Role[0]));
    }

    public static void setNickname(Member member, String nickname) {
        if (member == null) {
            DiscordSRV.debug("Can't set nickname of null member");
            return;
        }

        if (!member.getGuild().getSelfMember().canInteract(member)) {
            DiscordSRV.debug("Not setting " + member + "'s nickname because we can't interact with them");
            return;
        }

        if (nickname != null && nickname.equals(member.getNickname())) {
            DiscordSRV.debug("Not setting " + member + "'s nickname because it wouldn't change");
            return;
        }

        try {
            member.modifyNickname(nickname).queue();
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not set nickname for " + member + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not set nickname for " + member + " because \"" + e.getMessage() + "\"");
            }
        }
    }

    public static Role getRole(String roleId) {
        try {
            return getJda().getRoleById(roleId);
        } catch (Exception ignored) {
            return null;
        }
    }
    public static Role getRoleByName(Guild guild, String roleName) {
        return guild.getRoles().stream()
                .filter(role -> role.getName().equalsIgnoreCase(roleName))
                .findFirst()
                .orElse(null);
    }

    public static void banMember(Member member) {
        banMember(member, 0);
    }
    public static void banMember(Member member, int daysOfMessagesToDelete) {
        if (member == null) {
            DiscordSRV.debug("Attempted to ban null member");
            return;
        }

        daysOfMessagesToDelete = Math.abs(daysOfMessagesToDelete);

        try {
            member.ban(daysOfMessagesToDelete).queue();
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Failed to ban " + member + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Failed to ban " + member + " because \"" + e.getMessage() + "\"");
            }
        }
    }

    public static void unbanUser(Guild guild, User user) {
        try {
            guild.unban(user).queue(null, t -> DiscordSRV.error("Failed to unban user " + user + ": " + t.getMessage()));
        } catch (Exception e) {
            DiscordSRV.error("Failed to unban user " + user + ": " + e.getMessage());
        }
    }

    public static String translateEmotes(String messageToTranslate, Guild guild) {
        return translateEmotes(messageToTranslate, guild.getEmotes());
    }
    public static String translateEmotes(String messageToTranslate, List<Emote> emotes) {
        for (Emote emote : emotes)
            messageToTranslate = messageToTranslate.replace(":" + emote.getName() + ":", emote.getAsMention());
        return messageToTranslate;
    }

    public static TextChannel getTextChannelById(String channelId) {
        try {
            return getJda().getTextChannelById(channelId);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static Member getMemberById(String memberId) {
        try {
            return getJda().getGuilds().stream()
                    .filter(guild -> guild.getMemberById(memberId) != null)
                    .findFirst()
                    .map(guild -> guild.getMemberById(memberId))
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public static User getUserById(String userId) {
        try {
            return getJda().getUserById(userId);
        } catch (Exception ignored) {
            return null;
        }
    }
}
