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

package github.scarsz.discordsrv.listeners;

import com.vdurmont.emoji.EmojiParser;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.*;
import github.scarsz.discordsrv.hooks.DynmapHook;
import github.scarsz.discordsrv.hooks.VaultHook;
import github.scarsz.discordsrv.hooks.world.MultiverseCoreHook;
import github.scarsz.discordsrv.objects.SingleCommandSender;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static github.scarsz.discordsrv.util.MessageFormatResolver.getMessageFormat;

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

                DiscordUtil.sendMessage(event.getChannel(), MessageUtil.strip(discordMessage));
                return; // found a canned response, return so the message doesn't get processed further
            }
        }

        DiscordSRV.api.callEvent(new DiscordGuildMessageReceivedEvent(event));

        // if message from text channel other than a linked one return
        if (DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel()) == null) return;

        // sanity & intention checks
        String message = event.getMessage().getContentRaw();
        if (StringUtils.isBlank(message) && event.getMessage().getAttachments().size() == 0) return;
        if (processPlayerListCommand(event, message)) return;
        if (processConsoleCommand(event, event.getMessage().getContentRaw())) return;

        // return if should not send discord chat
        if (!DiscordSRV.config().getBoolean("DiscordChatChannelDiscordToMinecraft")) return;

        // enforce required account linking
        if (DiscordSRV.config().getBoolean("DiscordChatChannelRequireLinkedAccount")) {
            if (DiscordSRV.getPlugin().getAccountLinkManager() == null) {
                event.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(LangUtil.Message.FAILED_TO_CHECK_LINKED_ACCOUNT.toString()).queue());
                DiscordUtil.deleteMessage(event.getMessage());
                return;
            }

            boolean hasLinkedAccount = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getAuthor().getId()) != null;
            if (!hasLinkedAccount && !event.getAuthor().isBot()) {
                event.getAuthor().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(LangUtil.Message.LINKED_ACCOUNT_REQUIRED.toString()
                        .replace("%message%", event.getMessage().getContentRaw())
                ).queue());
                DiscordUtil.deleteMessage(event.getMessage());
                return;
            }
        }

        // block bots
        if (DiscordSRV.config().getBoolean("DiscordChatChannelBlockBots") && event.getAuthor().isBot()) {
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Received Discord message from bot " + event.getAuthor() + " but DiscordChatChannelBlockBots is on");
            return;
        }

        // blocked ids
        if (DiscordSRV.config().getStringList("DiscordChatChannelBlockedIds").contains(event.getAuthor().getId())) {
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Received Discord message from user " + event.getAuthor() + " but they are on the DiscordChatChannelBlockedIds list");
            return;
        }

        DiscordGuildMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new DiscordGuildMessagePreProcessEvent(event));
        if (preEvent.isCancelled()) {
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "DiscordGuildMessagePreProcessEvent was cancelled, message send aborted");
            return;
        }

        List<Role> selectedRoles = DiscordSRV.getPlugin().getSelectedRoles(event.getMember());
        Role topRole = !selectedRoles.isEmpty() ? selectedRoles.get(0) : null;

        // if there are attachments send them all as one message
        if (!event.getMessage().getAttachments().isEmpty()) {
            for (Message.Attachment attachment : event.getMessage().getAttachments().subList(0, Math.min(event.getMessage().getAttachments().size(), 3))) {

                // get the correct format message
                String destinationGameChannelNameForTextChannel = DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel());
                String placedMessage = getMessageFormat(selectedRoles, destinationGameChannelNameForTextChannel);

                placedMessage = MessageUtil.translateLegacy(
                        replacePlaceholders(placedMessage, event, selectedRoles, attachment.getUrl()));
                placedMessage = DiscordUtil.convertMentionsToNames(placedMessage);
                Component component = MessageUtil.toComponent(placedMessage);
                component = replaceTopRoleColor(component, topRole != null ? topRole.getColorRaw() : DiscordUtil.DISCORD_DEFAULT_COLOR.getRGB());

                DiscordGuildMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new DiscordGuildMessagePostProcessEvent(event, preEvent.isCancelled(), component));
                if (postEvent.isCancelled()) {
                    DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "DiscordGuildMessagePostProcessEvent was cancelled, attachment send aborted");
                    return;
                }
                DiscordSRV.getPlugin().broadcastMessageToMinecraftServer(DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel()), component, event.getAuthor());
                if (DiscordSRV.config().getBoolean("DiscordChatChannelBroadcastDiscordMessagesToConsole"))
                    DiscordSRV.info(LangUtil.InternalMessage.CHAT + ": " + MessageUtil.strip(MessageUtil.toLegacy(component).replace("»", ">")));
            }

            if (StringUtils.isBlank(event.getMessage().getContentRaw())) return;
        }

        // apply regex filters
        for (Map.Entry<Pattern, String> entry : DiscordSRV.getPlugin().getDiscordRegexes().entrySet()) {
            message = entry.getKey().matcher(message).replaceAll(entry.getValue());
            if (StringUtils.isBlank(message)) {
                DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Not processing Discord message because it was cleared by a filter: " + entry.getKey().pattern());
                return;
            }
        }

        if (message.length() > DiscordSRV.config().getInt("DiscordChatChannelTruncateLength")) {
            event.getMessage().addReaction("\uD83D\uDCAC").queue(v -> event.getMessage().addReaction("❗").queue());
            message = message.substring(0, DiscordSRV.config().getInt("DiscordChatChannelTruncateLength"));
        }

        // strip colors if role doesn't have permission
        List<String> rolesAllowedToColor = DiscordSRV.config().getStringList("DiscordChatChannelRolesAllowedToUseColorCodesInChat");
        boolean shouldStripColors = !rolesAllowedToColor.contains("@everyone");
        for (Role role : event.getMember().getRoles())
            if (rolesAllowedToColor.contains(role.getName()) || rolesAllowedToColor.contains(role.getId())) shouldStripColors = false;
        if (shouldStripColors) message = MessageUtil.stripLegacy(message);

        // get the correct format message
        String destinationGameChannelNameForTextChannel = DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel());
        String formatMessage = getMessageFormat(selectedRoles, destinationGameChannelNameForTextChannel);

        message = message != null ? message : "<blank message>";
        boolean isLegacy = MessageUtil.isLegacy(message) || MessageUtil.isLegacy(formatMessage);

        Component reserialized = MessageUtil.reserializeToMinecraftBasedOnConfig(message);
        message = shouldStripColors ? PlainTextComponentSerializer.plainText().serialize(reserialized) : MessageUtil.toPlain(reserialized, isLegacy);
        if (!isLegacy && shouldStripColors) message = MessageUtil.escapeMiniTokens(message);
        message = DiscordUtil.convertMentionsToNames(message);

        if (StringUtils.isBlank(message)) {
            // just emotes
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Ignoring message from " + event.getAuthor() + " because it became completely blank after reserialization (emote filtering)");
            return;
        }

        String emojiBehavior = DiscordSRV.config().getString("DiscordChatChannelEmojiBehavior");
        boolean hideEmoji = emojiBehavior.equalsIgnoreCase("hide");
        if (hideEmoji && StringUtils.isBlank(EmojiParser.removeAllEmojis(message))) {
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Ignoring message from " + event.getAuthor() + " because it became completely blank after removing unicode emojis");
            return;
        }

        String finalMessage = message;
        formatMessage = replacePlaceholders(formatMessage, event, selectedRoles, finalMessage);

        // translate color codes
        formatMessage = MessageUtil.translateLegacy(formatMessage);

        if (emojiBehavior.equalsIgnoreCase("show")) {
            // emojis already exist as unicode
        } else if (hideEmoji) {
            formatMessage = EmojiParser.removeAllEmojis(formatMessage);
        } else {
            // parse emojis from unicode back to :code:
            formatMessage = EmojiParser.parseToAliases(formatMessage);
        }

        // apply placeholder API values
        Player authorPlayer = null;
        UUID authorLinkedUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getAuthor().getId());
        if (authorLinkedUuid != null) authorPlayer = Bukkit.getPlayer(authorLinkedUuid);

        formatMessage = PlaceholderUtil.replacePlaceholders(formatMessage, authorPlayer);
        Component component = MessageUtil.toComponent(formatMessage);
        component = replaceTopRoleColor(component, topRole != null ? topRole.getColorRaw() : DiscordUtil.DISCORD_DEFAULT_COLOR.getRGB());

        DiscordGuildMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new DiscordGuildMessagePostProcessEvent(event, preEvent.isCancelled(), component));
        if (postEvent.isCancelled()) {
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "DiscordGuildMessagePostProcessEvent was cancelled, message send aborted");
            return;
        }

        DiscordSRV.getPlugin().getPluginHooks().stream()
                .filter(pluginHook -> pluginHook instanceof DynmapHook)
                .map(pluginHook -> (DynmapHook) pluginHook)
                .findAny().ifPresent(dynmapHook -> {
                    String chatFormat = replacePlaceholders(LangUtil.Message.DYNMAP_CHAT_FORMAT.toString(), event, selectedRoles, finalMessage);
                    String nameFormat = replacePlaceholders(LangUtil.Message.DYNMAP_NAME_FORMAT.toString(), event, selectedRoles, finalMessage);

                    chatFormat = MessageUtil.translateLegacy(chatFormat);
                    nameFormat = MessageUtil.translateLegacy(nameFormat);

                    if (emojiBehavior.equalsIgnoreCase("show")) {
                        // emojis already exist as unicode
                    } else if (hideEmoji) {
                        chatFormat = EmojiParser.removeAllEmojis(chatFormat);
                        nameFormat = EmojiParser.removeAllEmojis(nameFormat);
                    } else {
                        chatFormat = EmojiParser.parseToAliases(chatFormat);
                        nameFormat = EmojiParser.parseToAliases(nameFormat);
                    }

                    chatFormat = PlaceholderUtil.replacePlaceholders(chatFormat);
                    nameFormat = PlaceholderUtil.replacePlaceholders(nameFormat);

                    // apply regex filters
                    for (Map.Entry<Pattern, String> entry : DiscordSRV.getPlugin().getDiscordRegexes().entrySet()) {
                        chatFormat = entry.getKey().matcher(chatFormat).replaceAll(entry.getValue());
                        nameFormat = entry.getKey().matcher(nameFormat).replaceAll(entry.getValue());
                    }

                    nameFormat = MessageUtil.strip(nameFormat);
                    dynmapHook.broadcastMessageToDynmap(nameFormat, chatFormat);
        });

        DiscordSRV.getPlugin().broadcastMessageToMinecraftServer(
                DiscordSRV.getPlugin().getDestinationGameChannelNameForTextChannel(event.getChannel()),
                postEvent.getMinecraftMessage(),
                event.getAuthor()
        );

        if (DiscordSRV.config().getBoolean("DiscordChatChannelBroadcastDiscordMessagesToConsole")) {
            DiscordSRV.info(LangUtil.InternalMessage.CHAT + ": " + MessageUtil.strip(MessageUtil.toLegacy(postEvent.getMinecraftMessage()).replace("»", ">")));
        }
    }

    private static final Pattern TOP_ROLE_COLOR_PATTERN = Pattern.compile("%toprolecolor%.*"); // .* allows us the color the rest of the component
    private Component replaceTopRoleColor(Component component, int color) {
        return component
                .replaceText(TextReplacementConfig.builder()
                        .match(TOP_ROLE_COLOR_PATTERN)
                        .replacement(builder -> builder.content(builder.content().replaceFirst("%toprolecolor%", "")).color(TextColor.color(color)))
                        .build()
                );
    }

    private String getTopRoleAlias(Role role) {
        if (role == null) return "";
        String name = role.getName();
        return DiscordSRV.getPlugin().getRoleAliases().getOrDefault(role.getId(),
                DiscordSRV.getPlugin().getRoleAliases().getOrDefault(name.toLowerCase(), name)
        );
    }

    private String replacePlaceholders(String input, GuildMessageReceivedEvent event, List<Role> selectedRoles, String message) {
        Function<String, String> escape = MessageUtil.isLegacy(input)
                ? str -> str
                : str -> str.replaceAll("([<>])", "\\\\$1");

        return input.replace("%channelname%", event.getChannel().getName())
                .replace("%name%", escape.apply(MessageUtil.strip(event.getMember().getEffectiveName())))
                .replace("%username%", escape.apply(MessageUtil.strip(event.getMember().getUser().getName())))
                .replace("%toprole%", escape.apply(DiscordUtil.getRoleName(!selectedRoles.isEmpty() ? selectedRoles.get(0) : null)))
                .replace("%toproleinitial%", !selectedRoles.isEmpty() ? escape.apply(DiscordUtil.getRoleName(selectedRoles.get(0)).substring(0, 1)) : "")
                .replace("%toprolealias%", escape.apply(getTopRoleAlias(!selectedRoles.isEmpty() ? selectedRoles.get(0) : null)))
                .replace("%allroles%", escape.apply(DiscordUtil.getFormattedRoles(selectedRoles)))
                .replace("%reply%", event.getMessage().getReferencedMessage() != null ? replaceReplyPlaceholders(LangUtil.Message.CHAT_TO_MINECRAFT_REPLY.toString(), event.getMessage().getReferencedMessage()) : "")
                .replace("\\~", "~") // get rid of escaped characters, since Minecraft doesn't use markdown
                .replace("\\*", "*") // get rid of escaped characters, since Minecraft doesn't use markdown
                .replace("\\_", "_") // get rid of escaped characters, since Minecraft doesn't use markdown
                .replace("%message%", message);
    }

    private String replaceReplyPlaceholders(String format, Message repliedMessage) {
        Function<String, String> escape = MessageUtil.isLegacy(format)
                ? str -> str
                : str -> str.replaceAll("([<>])", "\\\\$1");

        final String repliedUserName = repliedMessage.getMember() != null ? repliedMessage.getMember().getEffectiveName() : repliedMessage.getAuthor().getName();

        return format.replace("%name%", escape.apply(MessageUtil.strip(repliedUserName)))
                .replace("%username%", escape.apply(MessageUtil.strip(repliedMessage.getAuthor().getName())))
                .replace("%message%", escape.apply(MessageUtil.strip(repliedMessage.getContentDisplay())));
    }

    private boolean processPlayerListCommand(GuildMessageReceivedEvent event, String message) {
        if (!DiscordSRV.config().getBoolean("DiscordChatChannelListCommandEnabled")) return false;
        if (!StringUtils.trimToEmpty(message).equalsIgnoreCase(DiscordSRV.config().getString("DiscordChatChannelListCommandMessage"))) return false;

        if (PlayerUtil.getOnlinePlayers(true).size() == 0) {
            DiscordUtil.sendMessage(event.getChannel(), PlaceholderUtil.replacePlaceholdersToDiscord(LangUtil.Message.PLAYER_LIST_COMMAND_NO_PLAYERS.toString()), DiscordSRV.config().getInt("DiscordChatChannelListCommandExpiration") * 1000);
        } else {
            String playerListMessage = "";
            playerListMessage += LangUtil.Message.PLAYER_LIST_COMMAND.toString().replace("%playercount%", PlayerUtil.getOnlinePlayers(true).size() + "/" + Bukkit.getMaxPlayers());
            playerListMessage = PlaceholderUtil.replacePlaceholdersToDiscord(playerListMessage);
            playerListMessage += "\n```\n";

            StringJoiner players = new StringJoiner(LangUtil.Message.PLAYER_LIST_COMMAND_ALL_PLAYERS_SEPARATOR.toString());

            List<String> playerList = new LinkedList<>();
            for (Player player : PlayerUtil.getOnlinePlayers(true)) {
                String userPrimaryGroup = VaultHook.getPrimaryGroup(player);
                boolean hasGoodGroup = StringUtils.isNotBlank(userPrimaryGroup);
                // capitalize the first letter of the user's primary group to look neater
                if (hasGoodGroup) userPrimaryGroup = userPrimaryGroup.substring(0, 1).toUpperCase() + userPrimaryGroup.substring(1);

                String playerFormat = LangUtil.Message.PLAYER_LIST_COMMAND_PLAYER.toString()
                        .replace("%username%", player.getName())
                        .replace("%displayname%", MessageUtil.strip(player.getDisplayName()))
                        .replace("%primarygroup%", userPrimaryGroup)
                        .replace("%world%", player.getWorld().getName())
                        .replace("%worldalias%", MessageUtil.strip(MultiverseCoreHook.getWorldAlias(player.getWorld().getName())));

                // use PlaceholderAPI if available
                playerFormat = PlaceholderUtil.replacePlaceholdersToDiscord(playerFormat, player);
                playerList.add(playerFormat);
            }

            playerList.sort(Comparator.naturalOrder());
            for (String playerFormat : playerList) {
                players.add(playerFormat);
            }
            playerListMessage += players.toString();

            if (playerListMessage.length() > 1996) playerListMessage = playerListMessage.substring(0, 1993) + "...";
            playerListMessage += "\n```";
            DiscordUtil.sendMessage(event.getChannel(), playerListMessage, DiscordSRV.config().getInt("DiscordChatChannelListCommandExpiration") * 1000);
        }

        // expire message after specified time
        if (DiscordSRV.config().getInt("DiscordChatChannelListCommandExpiration") > 0 && DiscordSRV.config().getBoolean("DiscordChatChannelListCommandExpirationDeleteRequest")) {
            new Thread(() -> {
                try {
                    Thread.sleep(DiscordSRV.config().getInt("DiscordChatChannelListCommandExpiration") * 1000L);
                } catch (InterruptedException ignored) {}
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
                        DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Failed to send DM to " + event.getAuthor() + ": " + t.getMessage());
                        event.getChannel().sendMessage(e).queue();
                    });
                }, t -> {
                    DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Failed to open DM conversation with " + event.getAuthor() + ": " + t.getMessage());
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
                        DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Failed to send DM to " + event.getAuthor() + ": " + t.getMessage());
                        event.getChannel().sendMessage(e).queue();
                    });
                }, t -> {
                    DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Failed to open DM conversation with " + event.getAuthor() + ": " + t.getMessage());
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

        DiscordConsoleCommandPreProcessEvent consoleEvent = DiscordSRV.api.callEvent(new DiscordConsoleCommandPreProcessEvent(event, command, false));

        // Stop the command from being run if an API user cancels the event
        if (consoleEvent.isCancelled()) return true;

        // It uses the command from the consoleEvent in case the API user wants to hijack/change it
        // at this point, the user has permission to run commands at all and is able to run the requested command, so do it
        Bukkit.getScheduler().runTask(DiscordSRV.getPlugin(), () -> Bukkit.getServer().dispatchCommand(new SingleCommandSender(event, Bukkit.getServer().getConsoleSender()), consoleEvent.getCommand()));

        DiscordSRV.api.callEvent(new DiscordConsoleCommandPostProcessEvent(event, consoleEvent.getCommand(), false));
        return true;
    }

}
