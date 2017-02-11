package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 11/7/2016
 * @at 1:59 AM
 */
public class DiscordUtil {

    /**
     * Get the Role's name. If the Role is null, an empty string is returned.
     * @param role
     * @return
     */
    public static String getRoleName(Role role) {
        return role == null ? "" : role.getName();
    }

    /**
     * Get the top hierarchical Role of the User in the Guild
     * @param user
     * @param guild
     * @return
     */
    public static Role getTopRole(User user, Guild guild) {
        Role highestRole = null;
        for (Role role : guild.getMember(user).getRoles()) {
            if (highestRole == null) highestRole = role;
            else if (highestRole.getPosition() < role.getPosition()) highestRole = role;
        }
        return highestRole;
    }
    /**
     * Get the top hierarchical Role of the Member
     * @param member
     * @return
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
     * @param message
     * @param guild
     * @return
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
     * Trigger a mass exodus of messages in the TextChannel given the bot has the Manage Messages permission
     * @param channel
     * @return
     */
    public static int purgeChannel(TextChannel channel) {
        if (!DiscordUtil.checkPermission(channel, Permission.MESSAGE_MANAGE)) {
            String message = "I was told to purge the current channel but I don't have the `Manage Messages` permission.";
            if (DiscordUtil.checkPermission(channel, Permission.MESSAGE_WRITE)) channel.sendMessage(message).queue();
            else DiscordSRV.getPlugin().getLogger().warning("");
            return -1;
        }

        //TODO work out a better system for this
        try {
            int deletions = 0;
            List<Message> messages = channel.getHistory().retrievePast(100).block();
            while (messages.size() == 100) {
                channel.deleteMessages(messages).block();
                deletions += messages.size();
                messages = channel.getHistory().retrievePast(100).block();
            }
            if (messages.size() > 1) channel.deleteMessages(messages).queue();
            if (messages.size() == 1) channel.deleteMessageById(messages.get(0).getId()).queue();
            deletions += messages.size();
            return deletions;
        } catch (RateLimitedException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Return the given String with Markdown escaped. Useful for sending things to Discord.
     * @param text
     * @return
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
        return text == null ? null : stripColorPattern.matcher(text).replaceAll("");
    }
    private static final Pattern stripColorPattern = Pattern.compile("(?i)" + String.valueOf('§') + "[0-9A-FK-OR]");

    /**
     * Send the given String message to the given TextChannel
     * @param channel
     * @param message
     */
    public static void sendMessage(TextChannel channel, String message) {
        sendMessage(channel, message, 0);
    }
    /**
     * Send the given String message to the given TextChannel that will expire
     * @param channel the TextChannel to send the message to
     * @param message the message to send to the TextChannel
     * @param expiration milliseconds until expiration of message. if this is 0, the message will not expire
     */
    public static void sendMessage(TextChannel channel, String message, int expiration) {
        if (channel == null || message == null || message.equals("") || channel.getJDA() == null ||
                !checkPermission(channel, channel.getJDA().getSelfUser(), Permission.MESSAGE_READ) ||
                !checkPermission(channel, channel.getJDA().getSelfUser(), Permission.MESSAGE_WRITE))
            return;

        message = DiscordUtil.stripColor(message);

        String overflow = null;
        if (message.length() > 2000) {
            DiscordSRV.getPlugin().getLogger().warning("Tried sending message with length of " + message.length() + " (" + (message.length() - 2000) + " over limit)");
            overflow = message.substring(2000);
            message = message.substring(0, 2000);
        }

        queueMessage(channel, message, m -> {
            if (expiration > 0 && checkPermission(channel, Permission.MESSAGE_MANAGE)) {
                try { Thread.sleep(expiration); } catch (InterruptedException e) { e.printStackTrace(); }
                if (checkPermission(channel, Permission.MESSAGE_MANAGE)) m.deleteMessage().queue();
                else DiscordSRV.getPlugin().getLogger().warning("Could not delete message in channel " + channel + ", no permission to manage messages");
            }
        });
        if (overflow != null) sendMessage(channel, overflow, expiration);
    }

    public static boolean checkPermission(Channel channel, Permission permission) {
        return checkPermission(channel, channel.getJDA().getSelfUser(), permission);
    }
    public static boolean checkPermission(Channel channel, User user, Permission permission) {
        return channel.getGuild().getMember(user).hasPermission(channel, permission);
    }

    public static Message blockMessage(TextChannel channel, String message) {
        return blockMessage(channel, new MessageBuilder().append(message).build());
    }
    public static Message blockMessage(TextChannel channel, Message message) {
        final Message[] returnedMessage = {null};
        channel.sendMessage(message).queue(message1 -> returnedMessage[0] = message1);
        while (returnedMessage == null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return returnedMessage[0];
    }
    public static void queueMessage(TextChannel channel, String message, Consumer<Message> consumer) {
        channel.sendMessage(message).queue(consumer);
    }
    public static void queueMessage(TextChannel channel, Message message, Consumer<Message> consumer) {
        channel.sendMessage(message).queue(consumer);
    }

    public static void setTextChannelTopic(TextChannel channel, String topic) {
        channel.getManager().setTopic(topic).queue();
    }

}