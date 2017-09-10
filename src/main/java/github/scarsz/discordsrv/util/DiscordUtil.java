/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2017 Austin Shapiro AKA Scarsz
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

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageSentEvent;
import github.scarsz.discordsrv.api.events.DiscordPrivateMessageSentEvent;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
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

    /**
     * Convert @mentions into Discord-compatible <@012345678901234567890> mentions
     * @param message Message to convert
     * @param guild Guild to find names to convert
     * @return Contents of the given message with names converted to mentions
     */
    public static String convertMentionsFromNames(String message, Guild guild) {
        if (!message.contains("@")) return message;
        List<String> splitMessage = new ArrayList<>(Arrays.asList(message.split("@| ")));
        for (Member member : guild.getMembers())
            for (String segment : splitMessage)
                if (member.getEffectiveName().toLowerCase().equals(segment.toLowerCase()))
                    splitMessage.set(splitMessage.indexOf(segment), member.getAsMention());
        splitMessage.removeAll(Arrays.asList("", null));
        return String.join(" ", splitMessage);
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
     * Strip the given String of Minecraft coloring. Useful for sending things to Discord.
     * @param text the given String to strip colors from
     * @return the given String with coloring stripped
     */
    public static String strip(String text) {
        if (text == null) {
            DiscordSRV.debug("Tried stripping null message");
            return null;
        }

        // standard regex-powered color stripping, bukkit's ChatColor::strip does this
        String newText = stripColorPattern.matcher(text).replaceAll("");

        // nuking the fuck out of it ourselves
        newText = newText.replaceAll("[&§][0-9a-fklmnor]", "");
        newText = newText.replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "");
        newText = newText.replaceAll("\\[[0-9]{1,3}m", "");
        newText = newText.replace("[m", "");

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
        return newText;
    }
    private static final Pattern stripColorPattern = Pattern.compile("(?i)" + String.valueOf('§') + "[0-9A-FK-OR]");

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

        if (editMessage) for (String phrase : DiscordSRV.config().getStringList("DiscordChatChannelCutPhrases"))
            message = message.replace(phrase, "");

        String overflow = null;
        if (message.length() > 2000) {
            DiscordSRV.debug("Tried sending message with length of " + message.length() + " (" + (message.length() - 2000) + " over limit)");
            overflow = message.substring(2000);
            message = message.substring(0, 2000);
        }

        queueMessage(channel, message, m -> {
            if (expiration > 0) {
                try { Thread.sleep(expiration); } catch (InterruptedException e) { e.printStackTrace(); }
                deleteMessage(m);
            }
        });
        if (overflow != null) sendMessage(channel, overflow, expiration, editMessage);
    }

    /**
     * Check if the bot has the given permission in the given channel
     * @param channel Channel to check for the permission in
     * @param permission Permission to be checked for
     * @return true if the permission is obtained, false otherwise
     */
    public static boolean checkPermission(Channel channel, Permission permission) {
        return checkPermission(channel, getJda().getSelfUser(), permission);
    }
    /**
     * Check if the bot has the given guild permission
     * @param guild Guild to check for the permission in
     * @param permission Permission to be checked for
     * @return true if the permission is obtained, false otherwise
     */
    public static boolean checkPermission(Guild guild, Permission permission) {
        return guild != null && guild.getMember(getJda().getSelfUser()).hasPermission(permission);
    }
    /**
     * Check if the given user has the given permission in the given channel
     * @param channel Channel to check for the permission in
     * @param user User to check permissions for
     * @param permission Permission to be checked for
     * @return true if the permission is obtained, false otherwise
     */
    public static boolean checkPermission(Channel channel, User user, Permission permission) {
        if (channel == null) return false;
        return channel.getGuild().getMember(user).hasPermission(channel, permission);
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
        if (channel == null) {
            DiscordSRV.debug("Tried sending a message to a null channel");
            return null;
        }

        if (message == null || StringUtils.isBlank(message.getRawContent())) {
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

        if (DiscordSRV.getPlugin().getConsoleChannel() != null && !channel.getId().equals(DiscordSRV.getPlugin().getConsoleChannel().getId()))
            DiscordSRV.getPlugin().getMetrics().increment("messages_sent_to_discord");

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

                if (DiscordSRV.getPlugin().getConsoleChannel() != null && !channel.getId().equals(DiscordSRV.getPlugin().getConsoleChannel().getId()))
                    DiscordSRV.getPlugin().getMetrics().increment("messages_sent_to_discord");
            });
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not send message in channel " + channel + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not send message in channel " + channel + " because \"" + e.getMessage() + "\"");
            }
        } catch (IllegalStateException ignored) {}
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
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not set topic of channel " + channel + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not set topic of channel " + channel + " because \"" + e.getMessage() + "\"");
            }
        } catch (IllegalStateException ignored) {}
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

        getJda().getPresence().setGame(Game.of(gameStatus));
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
        user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(message).queue(sentMessage -> DiscordSRV.api.callEvent(new DiscordPrivateMessageSentEvent(getJda(), sentMessage))));
    }

    public static boolean memberHasRole(Member member, List<String> rolesToCheck) {
        for (Role role : member.getRoles())
            for (String roleName : rolesToCheck)
                if (roleName.equalsIgnoreCase(role.getName())) return true;
        return false;
    }

    /**
     * Get the Minecraft-equivalent of the given Role for use with having corresponding colors
     * @param role The Role to look up
     * @return A String representing the Role's color in hex
     */
    public static String convertRoleToMinecraftColor(Role role) {
        if (role == null) {
            DiscordSRV.debug("Attempted to look up color for null roll");
            return "";
        }

        String hex = role.getColor() != null ? Integer.toHexString(role.getColor().getRGB()).toUpperCase() : "99AAB5";
        if (hex.length() == 8) hex = hex.substring(2);
        String translatedColor = DiscordSRV.getPlugin().getColors().get(hex);

        if (translatedColor == null) {
            DiscordSRV.debug("Attempted to lookup translated color " + hex + " for role " + role + " but no definition was found");
            translatedColor = "";
        }

        return translatedColor;
    }

    /**
     * Get a formatted String representing all of the Member's roles, delimited by DiscordToMinecraftAllRolesSeparator
     * @param member The Member to retrieve the roles of
     * @return The formatted String representing all of the Member's roles
     */
    public static String getAllRoles(Member member) {
        return String.join(LangUtil.Message.CHAT_TO_MINECRAFT_ALL_ROLES_SEPARATOR.toString(), member.getRoles().stream()
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

    public static void addRolesToMember(Member member, Role... roles) {
        List<Role> rolesToAdd = Arrays.stream(roles)
                .filter(role -> !role.isManaged())
                .filter(role -> !role.getGuild().getPublicRole().getId().equals(role.getId()))
                .collect(Collectors.toList());

        try {
            member.getGuild().getController().addRolesToMember(member, rolesToAdd).queue();
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not promote " + member + " to role(s) " + rolesToAdd + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not promote " + member + " to role(s) " + rolesToAdd + " because \"" + e.getMessage() + "\"");
            }
        }
    }
    public static void addRolesToMember(Member member, List<Role> rolesToAdd) {
        addRolesToMember(member, rolesToAdd.toArray(new Role[0]));
    }
    public static void removeRolesFromMember(Member member, Role... roles) {
        List<Role> rolesToRemove = Arrays.stream(roles)
                .filter(role -> !role.isManaged())
                .filter(role -> !role.getGuild().getPublicRole().getId().equals(role.getId()))
                .collect(Collectors.toList());

        try {
            member.getGuild().getController().removeRolesFromMember(member, rolesToRemove).queue();
        } catch (PermissionException e) {
            if (e.getPermission() != Permission.UNKNOWN) {
                DiscordSRV.warning("Could not demote " + member + " from role(s) " + rolesToRemove + " because the bot does not have the \"" + e.getPermission().getName() + "\" permission");
            } else {
                DiscordSRV.warning("Could not demote " + member + " from role(s) " + rolesToRemove + " because \"" + e.getMessage() + "\"");
            }
        }
    }
    public static void removeRolesFromMember(Member member, List<Role> rolesToRemove) {
        removeRolesFromMember(member, rolesToRemove.toArray(new Role[0]));
    }

    public static void setNickname(Member member, String nickname) {
        try {
            member.getGuild().getController().setNickname(member, nickname).queue();
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

    public static Role getRole(Guild guild, String roleName) {
        for (Role role : guild.getRoles())
            if (role.getName().equalsIgnoreCase(roleName))
                return role;
        return null;
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
            member.getGuild().getController().ban(member, daysOfMessagesToDelete).queue();
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
            guild.getController().unban(user);
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
        for (Guild guild : getJda().getGuilds()) {
            try {
                Member member = guild.getMemberById(memberId);
                if (member != null) return member; // member with matching id found
            } catch (Exception ignored) {
                return null; // Guild#getMemberById error'd, probably because invalid memberId
            }
        }

        return null; // no matching member found
    }

    public static User getUserById(String userId) {
        try {
            return getJda().getUserById(userId);
        } catch (Exception ignored) {
            return null;
        }
    }
}
