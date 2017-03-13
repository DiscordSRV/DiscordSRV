package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.objects.SingleCommandSender;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 6:12 PM
 */
public class DiscordChatListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        // if message is from null author or self do not process
        if (event.getAuthor() != null && event.getAuthor().getId() != null && DiscordUtil.getJda().getSelfUser().getId() != null && event.getAuthor().getId().equals(DiscordUtil.getJda().getSelfUser().getId())) return;

        DiscordSRV.api.callEvent(new DiscordGuildMessageReceivedEvent(event));

        // if message from text channel other than a linked one return
        if (DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel()) == null) return;

        DiscordGuildMessagePreProcessEvent preEvent = (DiscordGuildMessagePreProcessEvent) DiscordSRV.api.callEvent(new DiscordGuildMessagePreProcessEvent(event));

        if (StringUtils.isBlank(event.getMessage().getRawContent()) && event.getMessage().getAttachments().size() > 0) {
            for (Message.Attachment attachment : event.getMessage().getAttachments().subList(0, event.getMessage().getAttachments().size() > 3 ? 3 : 1)) {
                String message = ChatColor.translateAlternateColorCodes('&', (event.getMember().getRoles().isEmpty()
                        ? DiscordSRV.getPlugin().getConfig().getString("DiscordToMinecraftChatMessageFormatNoRole")
                        : DiscordSRV.getPlugin().getConfig().getString("DiscordToMinecraftChatMessageFormat"))
                        .replace("%message%", attachment.getUrl())
                        .replace("%username%", event.getMember().getEffectiveName())
                        .replace("%toprole%", DiscordUtil.getRoleName(DiscordUtil.getTopRole(event.getMember())))
                        .replace("%toprolecolor%", DiscordUtil.convertRoleToMinecraftColor(DiscordUtil.getTopRole(event.getMember())))
                        .replace("%allroles%", DiscordUtil.getAllRoles(event.getMember()))
                        .replace("\\~", "~") // get rid of badly escaped characters
                        .replace("\\*", "") // get rid of badly escaped characters
                        .replace("\\_", "_") // get rid of badly escaped characters
                );
                DiscordSRV.getPlugin().broadcastMessageToMinecraftServer(DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel()), message);
                if (DiscordSRV.getPlugin().getConfig().getBoolean("DiscordChatChannelBroadcastDiscordMessagesToConsole"))
                    DiscordSRV.info("Chat: " + DiscordUtil.stripColor(message.replace("»", ">")));
            }
        }

        // canned responses
        for (Map.Entry<String, String> entry : DiscordSRV.getPlugin().getResponses().entrySet()) {
            if (event.getMessage().getRawContent().toLowerCase().startsWith(entry.getKey().toLowerCase())) {
                DiscordUtil.sendMessage(event.getChannel(), entry.getValue());
                return; // found a canned response, return so the message doesn't get processed further
            }
        }

        // return if should not send discord chat
        if (!DiscordSRV.getPlugin().getConfig().getBoolean("DiscordChatChannelDiscordToMinecraft")) return;

        // if message contains a string that's suppose to make the entire message not be sent to discord, return
        for (String phrase : DiscordSRV.getPlugin().getConfig().getStringList("DiscordChatChannelBlockedPhrases")) if (event.getMessage().getContent().contains(phrase)) return;

        String message = event.getMessage().getStrippedContent();
        if (StringUtils.isBlank(message)) return;
        if (processPlayerListCommand(event, message)) return;
        if (processConsoleCommand(event, event.getMessage().getRawContent())) return;

        if (message.length() > DiscordSRV.getPlugin().getConfig().getInt("DiscordChatChannelTruncateLength")) message = message.substring(0, DiscordSRV.getPlugin().getConfig().getInt("DiscordChatChannelTruncateLength"));

        // get the correct format message
        String formatMessage = event.getMember().getRoles().isEmpty()
                ? DiscordSRV.getPlugin().getConfig().getString("DiscordToMinecraftChatMessageFormatNoRole")
                : DiscordSRV.getPlugin().getConfig().getString("DiscordToMinecraftChatMessageFormat")
        ;

        // strip colors if role doesn't have permission
        List<String> rolesAllowedToColor = DiscordSRV.getPlugin().getConfig().getStringList("DiscordChatChannelRolesAllowedToUseColorCodesInChat");
        boolean shouldStripColors = true;
        for (Role role : event.getMember().getRoles())
            if (rolesAllowedToColor.contains(role.getName())) shouldStripColors = false;
        if (shouldStripColors) message = DiscordUtil.stripColor(message);

        formatMessage = formatMessage
                .replace("%message%", message)
                .replace("%username%", event.getMember().getEffectiveName())
                .replace("%toprole%", DiscordUtil.getRoleName(DiscordUtil.getTopRole(event.getMember())))
                .replace("%toprolecolor%", DiscordUtil.convertRoleToMinecraftColor(DiscordUtil.getTopRole(event.getMember())))
                .replace("%allroles%", DiscordUtil.getAllRoles(event.getMember()))
                .replace("\\~", "~") // get rid of badly escaped characters
                .replace("\\*", "") // get rid of badly escaped characters
                .replace("\\_", "_"); // get rid of badly escaped characters

        // translate color codes
        formatMessage = ChatColor.translateAlternateColorCodes('&', formatMessage);

        DiscordGuildMessagePostProcessEvent postEvent = (DiscordGuildMessagePostProcessEvent) DiscordSRV.api.callEvent(new DiscordGuildMessagePostProcessEvent(event, preEvent.isCancelled(), formatMessage));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug("DiscordGuildMessagePostProcessEvent was cancelled, message send aborted");
            return;
        }


        DiscordSRV.getPlugin().broadcastMessageToMinecraftServer(DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel()), formatMessage);

        if (DiscordSRV.getPlugin().getConfig().getBoolean("DiscordChatChannelBroadcastDiscordMessagesToConsole")) {
            DiscordSRV.info("Chat: " + DiscordUtil.stripColor(formatMessage.replace("»", ">")));
        }
    }

    private boolean processPlayerListCommand(GuildMessageReceivedEvent event, String message) {
        if (!DiscordSRV.getPlugin().getConfig().getBoolean("DiscordChatChannelListCommandEnabled")) return false;
        if (!message.toLowerCase().startsWith(DiscordSRV.getPlugin().getConfig().getString("DiscordChatChannelListCommandMessage").toLowerCase())) return false;

        if (PlayerUtil.getOnlinePlayers().size() == 0) {
            DiscordUtil.sendMessage(event.getChannel(), DiscordSRV.getPlugin().getConfig().getString("DiscordChatChannelListCommandFormatNoOnlinePlayers"));
            return true;
        }

        List<String> onlinePlayers = new ArrayList<>();
        PlayerUtil.getOnlinePlayers().forEach(player -> onlinePlayers.add(DiscordUtil.stripColor(player.getDisplayName())));

        String playerlistMessage = "";
        playerlistMessage += "```\n";
        playerlistMessage += DiscordSRV.getPlugin().getConfig().getString("DiscordChatChannelListCommandFormatOnlinePlayers").replace("%playercount%", PlayerUtil.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
        playerlistMessage += "\n";
        playerlistMessage += String.join(", ", onlinePlayers);

        if (playerlistMessage.length() > 1996) playerlistMessage = playerlistMessage.substring(0, 1993) + "...";
        playerlistMessage += "\n```";
        DiscordUtil.sendMessage(event.getChannel(), playerlistMessage, DiscordSRV.getPlugin().getConfig().getInt("DiscordChatChannelListCommandExpiration") * 1000, true);

        // expire message after specified time
        if (DiscordSRV.getPlugin().getConfig().getInt("DiscordChatChannelListCommandExpiration") > 0 && DiscordSRV.getPlugin().getConfig().getBoolean("DiscordChatChannelListCommandExpirationDeleteRequest")) {
            try { Thread.sleep(DiscordSRV.getPlugin().getConfig().getInt("DiscordChatChannelListCommandExpiration") * 1000); } catch (InterruptedException e) { e.printStackTrace(); }
            DiscordUtil.deleteMessage(event.getMessage());
        }

        return true;
    }

    private boolean processConsoleCommand(GuildMessageReceivedEvent event, String message) {
        if (!DiscordSRV.getPlugin().getConfig().getBoolean("DiscordChatChannelConsoleCommandEnabled")) return false;

        String[] parts = message.split(" ", 2);

        if (parts.length < 2) return false;
        if (!parts[0].equalsIgnoreCase(DiscordSRV.getPlugin().getConfig().getString("DiscordChatChannelConsoleCommandPrefix"))) return false;

        // check if user has a role able to use this
        List<String> rolesAllowedToConsole = new ArrayList<>(DiscordSRV.getPlugin().getConfig().getStringList("DiscordChatChannelConsoleCommandRolesAllowed"));
        boolean allowed = DiscordUtil.memberHasRole(event.getMember(), rolesAllowedToConsole);
        if (!allowed) {
            // tell user that they have no permission
            if (DiscordSRV.getPlugin().getConfig().getBoolean("DiscordChatChannelConsoleCommandNotifyErrors"))
                DiscordUtil.privateMessage(event.getAuthor(), DiscordSRV.getPlugin().getConfig().getString("DiscordChatChannelConsoleCommandNotifyErrorsFormat")
                                .replace("%user%", event.getAuthor().getName())
                                .replace("%error%", "no permission")
                );
            return true;
        }

        // check if user has a role that can bypass the white/blacklist
        boolean canBypass = false;
        for (String roleName : DiscordSRV.getPlugin().getConfig().getStringList("DiscordChatChannelConsoleCommandWhitelistBypassRoles")) {
            boolean isAble = DiscordUtil.memberHasRole(event.getMember(), Collections.singletonList(roleName));
            canBypass = isAble || canBypass;
        }

        // check if requested command is white/blacklisted
        boolean commandIsAbleToBeUsed;

        if (canBypass) {
            commandIsAbleToBeUsed = true;
        }
        else {
            // Check the white/black list
            String requestedCommand = parts[1].split(" ")[0];
            boolean whitelistActsAsBlacklist = DiscordSRV.getPlugin().getConfig().getBoolean("DiscordChatChannelConsoleCommandWhitelistActsAsBlacklist");

            List<String> commandsToCheck = DiscordSRV.getPlugin().getConfig().getStringList("DiscordChatChannelConsoleCommandWhitelist");
            boolean isListed = commandsToCheck.contains(requestedCommand);

            commandIsAbleToBeUsed = isListed ^ whitelistActsAsBlacklist;
        }

        if (!commandIsAbleToBeUsed) {
            // tell user that the command is not able to be used
            if (DiscordSRV.getPlugin().getConfig().getBoolean("DiscordChatChannelConsoleCommandNotifyErrors"))
                 DiscordUtil.privateMessage(event.getAuthor(), DiscordSRV.getPlugin().getConfig().getString("DiscordChatChannelConsoleCommandNotifyErrorsFormat")
                         .replace("%user%", event.getAuthor().getName())
                         .replace("%error%", "command is not able to be used")
                 );
            return true;
        }

        // log command to console log file, if this fails the command is not executed for safety reasons unless this is turned off
        try {
            FileUtils.writeStringToFile(
                    new File(DiscordSRV.getPlugin().getConfig().getString("DiscordConsoleChannelUsageLog")),
                    "[" + TimeUtil.timeStamp() + " | ID " + event.getAuthor().getId() + "] " + event.getAuthor().getName() + ": " + event.getMessage().getContent() + System.lineSeparator(),
                    Charset.defaultCharset(),
                    true
            );
        } catch (IOException e) {
            DiscordSRV.error("Error logging console action to " + DiscordSRV.getPlugin().getConfig().getString("DiscordConsoleChannelUsageLog") + ": " + e.getLocalizedMessage());
            if (DiscordSRV.getPlugin().getConfig().getBoolean("CancelConsoleCommandIfLoggingFailed")) return true;
        }

        // at this point, the user has permission to run commands at all and is able to run the requested command, so do it
        Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), () -> Bukkit.getServer().dispatchCommand(new SingleCommandSender(event, Bukkit.getServer().getConsoleSender()), parts[1]));

        // increment metric
        DiscordSRV.getPlugin().getMetrics().get("console_commands_processed").incrementAndGet();

        return true;
    }

}
