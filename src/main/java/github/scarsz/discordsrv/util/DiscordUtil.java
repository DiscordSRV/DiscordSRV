package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageSentEvent;
import github.scarsz.discordsrv.api.events.DiscordPrivateMessageSentEvent;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 11/7/2016
 * @at 1:59 AM
 */
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
     * Get the top hierarchical Role of the User in the Guild
     * @param user User the get the top Role of
     * @param guild Guild that the Role should be the top of
     * @return The top hierarchical Role
     */
    public static Role getTopRole(User user, Guild guild) {
        return getTopRole(guild.getMember(user));
    }
    /**
     * Get the top hierarchical Role of the Member
     * @param member Member to get the top role of
     * @return The top hierarchical Role
     */
    public static Role getTopRole(Member member) {
        Role highestRole = null;
        for (Role role : member.getRoles()) {
            if (highestRole == null) highestRole = role;
            else if (highestRole.getPosition() < role.getPosition()) highestRole = role;
        }
        return highestRole;
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
        return text.replace("_", "\\_").replace("*", "\\*").replace("~", "\\~");
    }

    /**
     * Strip the given String of Minecraft coloring. Useful for sending things to Discord.
     * @param text the given String to strip colors from
     * @return the given String with coloring stripped
     */
    public static String stripColor(String text) {
        if (text == null) {
            DiscordSRV.debug("Tried stripping null message");
            return null;
        }

        // standard regex-powered color stripping, bukkit's ChatColor::stripColor does this
        String newText = stripColorPattern.matcher(text).replaceAll("");

        // nuking the fuck out of it ourselves
        newText = newText.replaceAll("[&§][0-9a-fklmnor]", "");
        newText = newText.replaceAll("\\[[0-9]{1,2};[0-9]{1,2};[0-9]{1,2}m", "");
        newText = newText.replaceAll("\\[[0-9]{1,3}m", "");
        newText = newText.replace("[m", "");

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

        if (!checkPermission(channel, Permission.MESSAGE_READ)) {
            DiscordSRV.debug("Tried sending a message to channel " + channel + " but the bot doesn't have read permissions for that channel");
            return;
        }

        if (!checkPermission(channel, Permission.MESSAGE_WRITE)) {
            DiscordSRV.debug("Tried sending a message to channel " + channel + " but the bot doesn't have write permissions for that channel");
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

        message = DiscordUtil.stripColor(message);

        if (editMessage) for (String phrase : DiscordSRV.getPlugin().getConfig().getStringList("DiscordChatChannelCutPhrases"))
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

        if (!DiscordUtil.checkPermission(channel, Permission.MESSAGE_READ)) {
            DiscordSRV.debug("Tried sending a message to channel " + channel + " of which the bot doesn't have read permission for");
            return null;
        }
        if (!DiscordUtil.checkPermission(channel, Permission.MESSAGE_WRITE)) {
            DiscordSRV.debug("Tried sending a message to channel " + channel + " of which the bot doesn't have write permission for");
            return null;
        }

        Message sentMessage = channel.sendMessage(message).complete();
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

        if (!DiscordUtil.checkPermission(channel, Permission.MESSAGE_READ)) {
            DiscordSRV.debug("Tried sending a message to channel " + channel + " of which the bot doesn't have read permission for");
            return;
        }
        if (!DiscordUtil.checkPermission(channel, Permission.MESSAGE_WRITE)) {
            DiscordSRV.debug("Tried sending a message to channel " + channel + " of which the bot doesn't have write permission for");
            return;
        }

        try {
            channel.sendMessage(message).queue(sentMessage -> {
                DiscordSRV.api.callEvent(new DiscordGuildMessageSentEvent(getJda(), sentMessage));
                consumer.accept(sentMessage);

                if (DiscordSRV.getPlugin().getConsoleChannel() != null && !channel.getId().equals(DiscordSRV.getPlugin().getConsoleChannel().getId()))
                    DiscordSRV.getPlugin().getMetrics().increment("messages_sent_to_discord");
            });
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

        if (!DiscordUtil.checkPermission(channel, Permission.MANAGE_CHANNEL)) {
            DiscordSRV.warning("Unable to update topic of " + channel + " because the bot is missing the \"Manage Channel\" permission. Did you follow the instructions?");
            return;
        }

        try {
            channel.getManager().setTopic(topic).queue();
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
        if (gameStatus == null || gameStatus.isEmpty()) {
            DiscordSRV.debug("Attempted setting game status to a null or empty string");
            return;
        }

        getJda().getPresence().setGame(Game.of(gameStatus));
    }

    /**
     * Delete the given message, given the bot has permission to
     * @param message The message to delete
     */
    public static void deleteMessage(Message message) {
        if (message.isFromType(ChannelType.PRIVATE)) return;

        if (!checkPermission(message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
            DiscordSRV.warning("Could not delete message in channel " + message.getTextChannel() + " because the bot doesn't have the \"Manage Messages\" permission");
            return;
        }

        message.delete().queue();
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
        String translatedColor = DiscordSRV.getPlugin().getColors().get(hex);

        if (translatedColor == null) {
            DiscordSRV.debug("Attempted to lookup translated color for role " + role + " but no definition was found");
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
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void setAvatarBlocking(File avatar) throws RuntimeException {
        try {
            getJda().getSelfUser().getManager().setAvatar(Icon.from(avatar)).complete();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static void addRolesToMember(Member member, Role... roles) {
        if (!DiscordUtil.checkPermission(member.getGuild(), Permission.MANAGE_ROLES)) {
            DiscordSRV.warning("Could not promote " + member + " to role(s) " + Arrays.toString(roles) + " because the bot does not have the \"Manage Roles\" permission");
            return;
        }

        if(!PermissionUtil.canInteract(DiscordSRV.getPlugin().getMainGuild().getSelfMember(), member)) {
            DiscordSRV.warning("Unable to set nickname of " + member + " because the bot is of lower ranking than them. Discord prevents you from modifying people higher than you.");
            return;
        }

        List<Role> rolesToAdd = new ArrayList<>();
        for (Role role : roles) {
            if (!PermissionUtil.canInteract(DiscordSRV.getPlugin().getMainGuild().getSelfMember(), role)) {
                DiscordSRV.warning("Unable to add role " + role + " to member " + member + " because the bot's highest role is lower than the role");
            } else if (role.getGuild().getPublicRole().getId().equals(role.getId())) {
                DiscordSRV.warning("Unable to add role " + role + " to member " + member + " because that is the public, unmodifiable role of the server");
            } else {
                rolesToAdd.add(role);
            }
        }

        member.getGuild().getController().addRolesToMember(member, rolesToAdd).queue();
    }
    public static void addRolesToMember(Member member, List<Role> rolesToAdd) {
        addRolesToMember(member, rolesToAdd.toArray(new Role[0]));
    }
    public static void removeRolesFromMember(Member member, Role... roles) {
        if (!DiscordUtil.checkPermission(member.getGuild(), Permission.MANAGE_ROLES)) {
            DiscordSRV.warning("Could not demote " + member + " from role(s) " + Arrays.toString(roles) + " because the bot does not have the \"Manage Roles\" permission");
            return;
        }

        if(!PermissionUtil.canInteract(DiscordSRV.getPlugin().getMainGuild().getSelfMember(), member)) {
            DiscordSRV.warning("Unable to remove role(s) " + Arrays.toString(roles) + " from member " + member + " because the bot is of lower ranking than them. Discord prevents you from modifying people higher than you.");
            return;
        }

        List<Role> rolesToRemove = new ArrayList<>();
        for (Role role : roles) {
            if (!PermissionUtil.canInteract(DiscordSRV.getPlugin().getMainGuild().getSelfMember(), role)) {
                DiscordSRV.warning("Unable to remove role " + role + " from member " + member + " because the bot's highest role is lower than the role");
            } else if (role.getGuild().getPublicRole().getId().equals(role.getId())) {
                DiscordSRV.warning("Unable to remove role " + role + " from member " + member + " because that is the public, unmodifiable role of the server");
            } else {
                rolesToRemove.add(role);
            }
        }

        member.getGuild().getController().removeRolesFromMember(member, rolesToRemove).queue();
    }
    public static void removeRolesFromMember(Member member, List<Role> rolesToRemove) {
        removeRolesFromMember(member, rolesToRemove.toArray(new Role[0]));
    }

    public static void setNickname(Member member, String nickname) {
        if (!DiscordUtil.checkPermission(member.getGuild(), Permission.NICKNAME_MANAGE)) {
            DiscordSRV.warning("Unable to set nickname of " + member + " because the bot is missing the \"Manage Nicknames\" permission.");
            return;
        }

        if(!PermissionUtil.canInteract(DiscordSRV.getPlugin().getMainGuild().getSelfMember(), member)) {
            DiscordSRV.warning("Unable to set nickname of " + member + " because the bot is of lower ranking than them. Discord prevents you from modifying people higher than you.");
            return;
        }

        member.getGuild().getController().setNickname(member, nickname).queue();
    }

    public static Role getRole(String roleId) {
        for (Guild guild : getJda().getGuilds()) {
            for (Role role : guild.getRoles()) {
                if (role.getId().equals(roleId)) {
                    return role;
                }
            }
        }
        return null;
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
        } catch (Exception e) {
            DiscordSRV.error("Failed to ban member " + member + ": " + e.getMessage());
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
        for (Emote emote : emotes.toArray(new Emote[0]))
            messageToTranslate = messageToTranslate.replace(":" + emote.getName() + ":", emote.getAsMention());
        return messageToTranslate;
    }

}