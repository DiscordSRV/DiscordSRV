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

package github.scarsz.discordsrv.listeners;

import com.vdurmont.emoji.EmojiParser;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.hooks.DynmapHook;
import github.scarsz.discordsrv.hooks.VaultHook;
import github.scarsz.discordsrv.hooks.world.MultiverseCoreHook;
import github.scarsz.discordsrv.objects.SingleCommandSender;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class DiscordChatListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        // if message is from null author or self do not process
        if (event.getMember() == null || DiscordUtil.getJda() == null || event.getAuthor().equals(DiscordUtil.getJda().getSelfUser()))
            return;

        // canned responses
        for (Map.Entry<String, String> entry : DiscordSRV.getPlugin().getCannedResponses().entrySet()) {
            if (event.getMessage().getContentRaw().toLowerCase().startsWith(entry.getKey().toLowerCase())) {
                String discordMessage = entry.getValue();
                discordMessage = PlaceholderUtil.replacePlaceholdersToDiscord(discordMessage);

                DiscordUtil.sendMessage(event.getChannel(), DiscordUtil.strip(discordMessage));
                return; // found a canned response, return so the message doesn't get processed further
            }
        }

        DiscordSRV.api.callEvent(new DiscordGuildMessageReceivedEvent(event));

        // if message from text channel other than a linked one return
        if (DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel()) == null) return;

        // sanity & intention checks
        String message = DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft") ? event.getMessage().getContentRaw() : event.getMessage().getContentStripped();
        if (StringUtils.isBlank(message) && event.getMessage().getAttachments().size() == 0) return;
        if (processPlayerListCommand(event, message)) return;
        if (processConsoleCommand(event, event.getMessage().getContentRaw())) return;

        // return if should not send discord chat
        if (!DiscordSRV.config().getBoolean("DiscordChatChannelDiscordToMinecraft")) return;

        // enforce required account linking
        if (DiscordSRV.config().getBoolean("DiscordChatChannelRequireLinkedAccount")) {
            if (DiscordSRV.getPlugin().getAccountLinkManager() == null) {
                event.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(LangUtil.InternalMessage.FAILED_TO_CHECK_LINKED_ACCOUNT.toString()).queue());
                DiscordUtil.deleteMessage(event.getMessage());
                return;
            }

            boolean hasLinkedAccount = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getAuthor().getId()) != null;
            if (!hasLinkedAccount && !event.getAuthor().isBot()) {
                event.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(LangUtil.InternalMessage.LINKED_ACCOUNT_REQUIRED.toString()
                        .replace("{message}", event.getMessage().getContentRaw())
                ).queue());
                DiscordUtil.deleteMessage(event.getMessage());
                return;
            }
        }

        // block bots
        if (DiscordSRV.config().getBoolean("DiscordChatChannelBlockBots") && event.getAuthor().isBot()) {
            DiscordSRV.debug("Received Discord message from bot " + event.getAuthor() + " but DiscordChatChannelBlockBots is on");
            return;
        }

        // blocked ids
        if (DiscordSRV.config().getStringList("DiscordChatChannelBlockedIds").contains(event.getAuthor().getId())) {
            DiscordSRV.debug("Received Discord message from user " + event.getAuthor() + " but they are on the DiscordChatChannelBlockedIds list");
            return;
        }

        DiscordGuildMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new DiscordGuildMessagePreProcessEvent(event));
        if (preEvent.isCancelled()) {
            DiscordSRV.debug("DiscordGuildMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }

        List<Role> selectedRoles;
        List<String> discordRolesSelection = DiscordSRV.config().getStringList("DiscordChatChannelRolesSelection");
        // if we have a whitelist in the config
        if (DiscordSRV.config().getBoolean("DiscordChatChannelRolesSelectionAsWhitelist")) {
            selectedRoles = event.getMember().getRoles().stream()
                               .filter(role -> discordRolesSelection.contains(DiscordUtil.getRoleName(role)))
                               .collect(Collectors.toList());
        } else { // if we have a blacklist in the settings
            selectedRoles = event.getMember().getRoles().stream()
                               .filter(role -> !discordRolesSelection.contains(DiscordUtil.getRoleName(role)))
                               .collect(Collectors.toList());
        }
        selectedRoles.removeIf(role -> StringUtils.isBlank(role.getName()));

        // if there are attachments send them all as one message
        if (!event.getMessage().getAttachments().isEmpty()) {
            for (Message.Attachment attachment : event.getMessage().getAttachments().subList(0, Math.min(event.getMessage().getAttachments().size(), 3))) {

                // get the correct format message
                String placedMessage = !selectedRoles.isEmpty()
                        ? LangUtil.Message.CHAT_TO_MINECRAFT.toString()
                        : LangUtil.Message.CHAT_TO_MINECRAFT_NO_ROLE.toString();

                placedMessage = ChatColor.translateAlternateColorCodes('&',
                        replacePlaceholders(placedMessage, event, selectedRoles, attachment.getUrl())
                );
                if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft")) placedMessage = DiscordUtil.convertMentionsToNames(placedMessage);
                DiscordGuildMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new DiscordGuildMessagePostProcessEvent(event, preEvent.isCancelled(), placedMessage));
                if (postEvent.isCancelled()) {
                    DiscordSRV.debug("DiscordGuildMessagePostProcessEvent was cancelled, attachment send aborted");
                    return;
                }
                DiscordSRV.getPlugin().broadcastMessageToMinecraftServer(DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel()), placedMessage, event.getAuthor());
                if (DiscordSRV.config().getBoolean("DiscordChatChannelBroadcastDiscordMessagesToConsole"))
                    DiscordSRV.info(LangUtil.InternalMessage.CHAT + ": " + DiscordUtil.strip(placedMessage.replace("»", ">")));
            }

            if (StringUtils.isBlank(event.getMessage().getContentRaw())) return;
        }

        // if message contains a string that's suppose to make the entire message not be sent to discord, return
        for (String phrase : DiscordSRV.config().getStringList("DiscordChatChannelBlockedPhrases")) {
            if (StringUtils.isEmpty(phrase)) continue; // don't want to block every message from sending
            if (event.getMessage().getContentRaw().contains(phrase)) {
                DiscordSRV.debug("Received message from Discord that contained a block phrase (" + phrase + "), message send aborted");
                return;
            }
        }

        if (message.length() > DiscordSRV.config().getInt("DiscordChatChannelTruncateLength")) {
            event.getMessage().addReaction("\uD83D\uDCAC").queue(v -> event.getMessage().addReaction("❗").queue());
            message = message.substring(0, DiscordSRV.config().getInt("DiscordChatChannelTruncateLength"));
        }

        // strip colors if role doesn't have permission
        List<String> rolesAllowedToColor = DiscordSRV.config().getStringList("DiscordChatChannelRolesAllowedToUseColorCodesInChat");
        boolean shouldStripColors = true;
        for (Role role : event.getMember().getRoles())
            if (rolesAllowedToColor.contains(role.getName())) shouldStripColors = false;
        if (shouldStripColors) message = DiscordUtil.strip(message);

        // get the correct format message
        String formatMessage = !selectedRoles.isEmpty()
                ? LangUtil.Message.CHAT_TO_MINECRAFT.toString()
                : LangUtil.Message.CHAT_TO_MINECRAFT_NO_ROLE.toString();

        String finalMessage = message != null ? message : "<blank message>";
        formatMessage = replacePlaceholders(formatMessage, event, selectedRoles, finalMessage);

        // translate color codes
        formatMessage = ChatColor.translateAlternateColorCodes('&', formatMessage);

        // parse emojis from unicode back to :code:
        if (DiscordSRV.config().getBoolean("ParseEmojisToNames")) {
            formatMessage = EmojiParser.parseToAliases(formatMessage);
        } else {
            formatMessage = EmojiParser.removeAllEmojis(formatMessage);
        }

        if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft")) formatMessage = DiscordUtil.convertMentionsToNames(formatMessage);

        DiscordGuildMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new DiscordGuildMessagePostProcessEvent(event, preEvent.isCancelled(), formatMessage));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug("DiscordGuildMessagePostProcessEvent was cancelled, message send aborted");
            return;
        }

        DiscordSRV.getPlugin().getPluginHooks().stream()
                .filter(pluginHook -> pluginHook instanceof DynmapHook)
                .map(pluginHook -> (DynmapHook) pluginHook)
                .findAny().ifPresent(dynmapHook -> {
                    String chatFormat = replacePlaceholders(LangUtil.Message.DYNMAP_CHAT_FORMAT.toString(), event, selectedRoles, finalMessage);
                    String nameFormat = replacePlaceholders(LangUtil.Message.DYNMAP_NAME_FORMAT.toString(), event, selectedRoles, finalMessage);

                    chatFormat = ChatColor.translateAlternateColorCodes('&', chatFormat);
                    nameFormat = ChatColor.translateAlternateColorCodes('&', nameFormat);

                    if (!DiscordSRV.config().getBoolean("ParseEmojisToNames")) {
                        chatFormat = EmojiParser.removeAllEmojis(chatFormat);
                        nameFormat = EmojiParser.removeAllEmojis(nameFormat);
                    }

                    if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft")) {
                        chatFormat = DiscordUtil.convertMentionsToNames(chatFormat);
                        nameFormat = DiscordUtil.convertMentionsToNames(nameFormat);
                    }

                    chatFormat = PlaceholderUtil.replacePlaceholders(chatFormat);
                    nameFormat = PlaceholderUtil.replacePlaceholders(nameFormat);

                    String regex = DiscordSRV.config().getString("DiscordChatChannelRegex");
                    if (StringUtils.isNotBlank(regex)) {
                        String replacement = DiscordSRV.config().getString("DiscordChatChannelRegexReplacement");
                        chatFormat = chatFormat.replaceAll(regex, replacement);
                        nameFormat = nameFormat.replaceAll(regex, replacement);
                    }

                    nameFormat = DiscordUtil.strip(nameFormat);

                    dynmapHook.broadcastMessageToDynmap(nameFormat, chatFormat);
        });

        DiscordSRV.getPlugin().broadcastMessageToMinecraftServer(DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel()), postEvent.getProcessedMessage(), event.getAuthor());

        if (DiscordSRV.config().getBoolean("DiscordChatChannelBroadcastDiscordMessagesToConsole")) {
            DiscordSRV.info(LangUtil.InternalMessage.CHAT + ": " + DiscordUtil.strip(postEvent.getProcessedMessage().replace("»", ">")));
        }
    }

    private String replacePlaceholders(String input, GuildMessageReceivedEvent event, List<Role> selectedRoles, String message) {
        return input.replace("%message%", message)
                .replace("%channelname%", event.getChannel().getName())
                .replace("%name%", DiscordUtil.strip(event.getMember().getEffectiveName()))
                .replace("%username%", DiscordUtil.strip(event.getMember().getUser().getName()))
                .replace("%toprole%", DiscordUtil.getRoleName(!selectedRoles.isEmpty() ? selectedRoles.get(0) : null))
                .replace("%toproleinitial%", !selectedRoles.isEmpty() ? DiscordUtil.getRoleName(selectedRoles.get(0)).substring(0, 1) : "")
                .replace("%toprolecolor%", DiscordUtil.convertRoleToMinecraftColor(!selectedRoles.isEmpty() ? selectedRoles.get(0) : null))
                .replace("%allroles%", DiscordUtil.getFormattedRoles(selectedRoles))
                .replace("\\~", "~") // get rid of badly escaped characters
                .replace("\\*", "") // get rid of badly escaped characters
                .replace("\\_", "_"); // get rid of badly escaped characters
    }

    private boolean processPlayerListCommand(GuildMessageReceivedEvent event, String message) {
        if (!DiscordSRV.config().getBoolean("DiscordChatChannelListCommandEnabled")) return false;
        if (!StringUtils.trimToEmpty(message).equalsIgnoreCase(DiscordSRV.config().getString("DiscordChatChannelListCommandMessage"))) return false;

        if (PlayerUtil.getOnlinePlayers(true).size() == 0) {
            DiscordUtil.sendMessage(event.getChannel(), LangUtil.Message.PLAYER_LIST_COMMAND_NO_PLAYERS.toString(), DiscordSRV.config().getInt("DiscordChatChannelListCommandExpiration") * 1000, true);
        } else {
            String playerListMessage = "";
            playerListMessage += LangUtil.Message.PLAYER_LIST_COMMAND.toString().replace("%playercount%", PlayerUtil.getOnlinePlayers(true).size() + "/" + Bukkit.getMaxPlayers());
            playerListMessage += "\n```\n";

            StringJoiner players = new StringJoiner(LangUtil.Message.PLAYER_LIST_COMMAND_ALL_PLAYERS_SEPARATOR.toString());
            for (Player player : PlayerUtil.getOnlinePlayers(true)) {

                String userPrimaryGroup = VaultHook.getPrimaryGroup(player);
                boolean hasGoodGroup = StringUtils.isNotBlank(userPrimaryGroup);
                // capitalize the first letter of the user's primary group to look neater
                if (hasGoodGroup) userPrimaryGroup = userPrimaryGroup.substring(0, 1).toUpperCase() + userPrimaryGroup.substring(1);

                String playerFormat = LangUtil.Message.PLAYER_LIST_COMMAND_PLAYER.toString()
                        .replace("%username%", DiscordUtil.strip(player.getName()))
                        .replace("%displayname%", DiscordUtil.strip(player.getDisplayName()))
                        .replace("%primarygroup%", userPrimaryGroup)
                        .replace("%world%", player.getWorld().getName())
                        .replace("%worldalias%", DiscordUtil.strip(MultiverseCoreHook.getWorldAlias(player.getWorld().getName())));

                // use PlaceholderAPI if available
                playerFormat = PlaceholderUtil.replacePlaceholdersToDiscord(playerFormat, player);

                players.add(playerFormat);
            }
            playerListMessage += players.toString();

            if (playerListMessage.length() > 1996) playerListMessage = playerListMessage.substring(0, 1993) + "...";
            playerListMessage += "\n```";
            DiscordUtil.sendMessage(event.getChannel(), playerListMessage, DiscordSRV.config().getInt("DiscordChatChannelListCommandExpiration") * 1000, true);
        }

        // expire message after specified time
        if (DiscordSRV.config().getInt("DiscordChatChannelListCommandExpiration") > 0 && DiscordSRV.config().getBoolean("DiscordChatChannelListCommandExpirationDeleteRequest")) {
            new Thread(() -> {
                try {
                    Thread.sleep(DiscordSRV.config().getInt("DiscordChatChannelListCommandExpiration") * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                DiscordUtil.deleteMessage(event.getMessage());
            }).start();
        }
        return true;
    }

    private boolean processConsoleCommand(GuildMessageReceivedEvent event, String message) {
        if (!DiscordSRV.config().getBoolean("DiscordChatChannelConsoleCommandEnabled")) return false;

        String prefix = DiscordSRV.config().getString("DiscordChatChannelConsoleCommandPrefix");
        if (!StringUtils.startsWithIgnoreCase(message, prefix)) return false;
        String command = message.substring(prefix.length()).trim();

        // check if user has a role able to use this
        Set<String> rolesAllowedToConsole = new HashSet<>();
        rolesAllowedToConsole.addAll(DiscordSRV.config().getStringList("DiscordChatChannelConsoleCommandRolesAllowed"));
        rolesAllowedToConsole.addAll(DiscordSRV.config().getStringList("DiscordChatChannelConsoleCommandWhitelistBypassRoles"));
        boolean allowed = event.isWebhookMessage() || DiscordUtil.memberHasRole(event.getMember(), rolesAllowedToConsole);
        if (!allowed) {
            // tell user that they have no permission
            if (DiscordSRV.config().getBoolean("DiscordChatChannelConsoleCommandNotifyErrors")) {
                String e = LangUtil.Message.CHAT_CHANNEL_COMMAND_ERROR.toString()
                        .replace("%user%", event.getAuthor().getName())
                        .replace("%error%", "no permission");
                event.getAuthor().openPrivateChannel().queue(dm -> {
                    dm.sendMessage(e).queue(null, t -> {
                        DiscordSRV.debug("Failed to send DM to " + event.getAuthor() + ": " + t.getMessage());
                        event.getChannel().sendMessage(e).queue();
                    });
                }, t -> {
                    DiscordSRV.debug("Failed to open DM conversation with " + event.getAuthor() + ": " + t.getMessage());
                    event.getChannel().sendMessage(e).queue();
                });
            }
            return true;
        }

        // check if user has a role that can bypass the white/blacklist
        boolean canBypass = false;
        for (String roleName : DiscordSRV.config().getStringList("DiscordChatChannelConsoleCommandWhitelistBypassRoles")) {
            boolean isAble = DiscordUtil.memberHasRole(event.getMember(), Collections.singleton(roleName));
            canBypass = isAble || canBypass;
        }

        // check if requested command is white/blacklisted
        boolean commandIsAbleToBeUsed;

        if (canBypass) {
            commandIsAbleToBeUsed = true;
        } else {
            // Check the white/black list
            String requestedCommand = command.split(" ")[0];
            boolean whitelistActsAsBlacklist = DiscordSRV.config().getBoolean("DiscordChatChannelConsoleCommandWhitelistActsAsBlacklist");

            List<String> commandsToCheck = DiscordSRV.config().getStringList("DiscordChatChannelConsoleCommandWhitelist");
            boolean isListed = commandsToCheck.contains(requestedCommand);

            commandIsAbleToBeUsed = isListed ^ whitelistActsAsBlacklist;
        }

        if (!commandIsAbleToBeUsed) {
            // tell user that the command is not able to be used
            if (DiscordSRV.config().getBoolean("DiscordChatChannelConsoleCommandNotifyErrors")) {
                String e = LangUtil.Message.CHAT_CHANNEL_COMMAND_ERROR.toString()
                        .replace("%user%", event.getAuthor().getName())
                        .replace("%error%", "command is not able to be used");
                event.getAuthor().openPrivateChannel().queue(dm -> {
                    dm.sendMessage(e).queue(null, t -> {
                        DiscordSRV.debug("Failed to send DM to " + event.getAuthor() + ": " + t.getMessage());
                        event.getChannel().sendMessage(e).queue();
                    });
                }, t -> {
                    DiscordSRV.debug("Failed to open DM conversation with " + event.getAuthor() + ": " + t.getMessage());
                    event.getChannel().sendMessage(e).queue();
                });
            }
            return true;
        }

        // log command to console log file, if this fails the command is not executed for safety reasons unless this is turned off
        File logFile = DiscordSRV.getPlugin().getLogFile();
        if (logFile != null) {
            try {
                FileUtils.writeStringToFile(
                    logFile,
                    "[" + TimeUtil.timeStamp() + " | ID " + event.getAuthor().getId() + "] " + event.getAuthor().getName() + ": " + event.getMessage().getContentRaw() + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    true
                );
            } catch (IOException e) {
                DiscordSRV.error(LangUtil.InternalMessage.ERROR_LOGGING_CONSOLE_ACTION + " " + logFile.getAbsolutePath() + ": " + e.getMessage());
                if (DiscordSRV.config().getBoolean("CancelConsoleCommandIfLoggingFailed")) return true;
            }
        }

        // at this point, the user has permission to run commands at all and is able to run the requested command, so do it
        Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), () -> Bukkit.getServer().dispatchCommand(new SingleCommandSender(event, Bukkit.getServer().getConsoleSender()), command));

        return true;
    }

}
