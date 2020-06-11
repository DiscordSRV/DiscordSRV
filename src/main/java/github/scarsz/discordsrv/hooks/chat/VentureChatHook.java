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

package github.scarsz.discordsrv.hooks.chat;

import com.comphenix.protocol.events.PacketContainer;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.*;
import mineverse.Aust1n46.chat.MineverseChat;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import mineverse.Aust1n46.chat.api.events.VentureChatEvent;
import mineverse.Aust1n46.chat.channel.ChatChannel;
import mineverse.Aust1n46.chat.utilities.Format;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.text.Component;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

public class VentureChatHook implements ChatHook {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVentureChat(VentureChatEvent event) {
        boolean shouldUseBungee = DiscordSRV.config().getBoolean("VentureChatBungee");

        ChatChannel chatChannel = event.getChannel();
        if (chatChannel == null) return; // uh oh, ok then

        boolean bungeeSend = event.isBungee();
        boolean bungeeReceive = !bungeeSend && chatChannel.getBungee();

        if (shouldUseBungee) {
            // event will fire again when received. don't want to process it twice for the sending server
            if (bungeeSend) return;
        } else {
            // since bungee compatability is disabled, we don't care about messages that we receive
            if (bungeeReceive) return;
        }

        String message = event.getChat();

        MineverseChatPlayer chatPlayer = event.getMineverseChatPlayer();
        if (chatPlayer != null) {
            Player player = chatPlayer.getPlayer();
            if (player != null) {
                // these events are never cancelled
                DiscordSRV.getPlugin().processChatMessage(player, message, chatChannel.getName(), false);
                return;
            }
        }

        if (!shouldUseBungee) {
            DiscordSRV.debug("Received a VentureChat message with a null MineverseChatPlayer or Player (and BungeeCord is disabled)");
            return;
        }

        // Below is copied from DiscordSRV#processChatMessage for supporting messages with no player

        DiscordSRV.debug("Processing VentureChat message without a Player object" + (bungeeReceive ? " (a BungeeCord receive)" : " (not a BungeeCord receive)"));
        if (!DiscordSRV.config().getBoolean("DiscordChatChannelMinecraftToDiscord")) {
            DiscordSRV.debug("A VentureChat message was received but it was not delivered to Discord because DiscordChatChannelMinecraftToDiscord is false");
            return;
        }

        String prefix = DiscordSRV.config().getString("DiscordChatChannelPrefixRequiredToProcessMessage");
        if (!DiscordUtil.strip(message).startsWith(prefix)) {
            DiscordSRV.debug("A VentureChat message was received but it was not delivered to Discord because the message didn't start with \"" + prefix + "\" (DiscordChatChannelPrefixRequiredToProcessMessage): \"" + message + "\"");
            return;
        }

        String userPrimaryGroup = event.getPlayerPrimaryGroup();
        if (userPrimaryGroup.equals("default")) userPrimaryGroup = "";

        boolean hasGoodGroup = StringUtils.isNotBlank(userPrimaryGroup);

        // capitalize the first letter of the user's primary group to look neater
        if (hasGoodGroup) userPrimaryGroup = userPrimaryGroup.substring(0, 1).toUpperCase() + userPrimaryGroup.substring(1);

        boolean reserializer = DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToDiscord");

        String username = DiscordUtil.strip(event.getUsername());
        if (!reserializer) username = DiscordUtil.escapeMarkdown(username);

        String channel = chatChannel.getName();

        String discordMessage = (hasGoodGroup
                ? LangUtil.Message.CHAT_TO_DISCORD.toString()
                : LangUtil.Message.CHAT_TO_DISCORD_NO_PRIMARY_GROUP.toString())
                .replaceAll("%time%|%date%", TimeUtil.timeStamp())
                .replace("%channelname%", channel != null ? channel.substring(0, 1).toUpperCase() + channel.substring(1) : "")
                .replace("%primarygroup%", userPrimaryGroup)
                .replace("%username%", username);
        discordMessage = PlaceholderUtil.replacePlaceholdersToDiscord(discordMessage);

        String displayName = DiscordUtil.strip(event.getNickname());
        if (reserializer) {
            message = DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacy().deserialize(message));
        } else {
            displayName = DiscordUtil.escapeMarkdown(displayName);
        }

        discordMessage = discordMessage
                .replace("%displayname%", displayName)
                .replace("%message%", message);

        if (!reserializer) discordMessage = DiscordUtil.strip(discordMessage);

        if (DiscordSRV.config().getBoolean("DiscordChatChannelTranslateMentions")) {
            discordMessage = DiscordUtil.convertMentionsFromNames(discordMessage, DiscordSRV.getPlugin().getMainGuild());
        } else {
            discordMessage = discordMessage.replace("@", "@\u200B"); // zero-width space
            message = message.replace("@", "@\u200B"); // zero-width space
        }

        if (!DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageDelivery")) {
            if (channel == null) {
                DiscordUtil.sendMessage(DiscordSRV.getPlugin().getMainTextChannel(), discordMessage);
            } else {
                DiscordUtil.sendMessage(DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel), discordMessage);
            }
        } else {
            if (channel == null) channel = DiscordSRV.getPlugin().getMainChatChannel();

            TextChannel destinationChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);
            if (destinationChannel == null) {
                DiscordSRV.debug("Failed to find Discord channel to forward message from game channel " + channel);
                return;
            }

            if (!DiscordUtil.checkPermission(destinationChannel.getGuild(), Permission.MANAGE_WEBHOOKS)) {
                DiscordSRV.error("Couldn't deliver chat message as webhook because the bot lacks the \"Manage Webhooks\" permission.");
                return;
            }

            message = PlaceholderUtil.replacePlaceholdersToDiscord(message);
            if (!reserializer) message = DiscordUtil.strip(message);

            message = DiscordUtil.cutPhrases(message);
            if (DiscordSRV.config().getBoolean("DiscordChatChannelTranslateMentions")) message = DiscordUtil.convertMentionsFromNames(message, DiscordSRV.getPlugin().getMainGuild());

            String webhookUsername = DiscordSRV.config().getString("Experiment_WebhookChatMessageUsernameFormat")
                    .replaceAll("(?:%displayname%)|(?:%username%)", DiscordUtil.strip(event.getUsername()));
            webhookUsername = PlaceholderUtil.replacePlaceholders(webhookUsername);
            webhookUsername = DiscordUtil.strip(webhookUsername);

            WebhookUtil.deliverMessage(destinationChannel, webhookUsername, DiscordSRV.getPlugin().getEmbedAvatarUrl(username, chatPlayer != null ? chatPlayer.getUUID() : null), message, null);
        }
    }

    @Override
    public void broadcastMessageToChannel(String channel, String message) {
        ChatChannel chatChannel = ChatChannel.getChannel(channel); // case in-sensitive
        if (chatChannel == null) {
            DiscordSRV.debug("Attempted to broadcast message to channel \"" + channel + "\" but the channel doesn't exist (returned null); aborting message send");
            return;
        }

        message = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", ChatColor.valueOf(chatChannel.getColor().toUpperCase()).toString())
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", chatChannel.getAlias())
                .replace("%message%", message);

        if (!DiscordSRV.config().getStringList("DisabledPluginHooks").contains("VentureChat-Bungee") && chatChannel.getBungee()) {
            if (chatChannel.isFiltered()) message = Format.FilterChat(message);

            if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft")) {
                message = LegacyComponentSerializer.legacy().serialize(MinecraftSerializer.INSTANCE.serialize(message));
            }
            MineverseChat.sendDiscordSRVPluginMessage(chatChannel.getName(), message);
        } else {
            List<MineverseChatPlayer> playersToNotify = MineverseChat.onlinePlayers.stream()
                    .filter(p -> p.getListening().contains(chatChannel.getName()))
                    .filter(p -> !chatChannel.hasPermission() || p.getPlayer().hasPermission(chatChannel.getPermission()))
                    .collect(Collectors.toList());
            for (MineverseChatPlayer player : playersToNotify) {
                String playerMessage = (player.hasFilter() && chatChannel.isFiltered()) ? Format.FilterChat(message) : message;

                if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft")) {
                    Component component = MinecraftSerializer.INSTANCE.serialize(playerMessage);
                    TextAdapter.sendComponent(player.getPlayer(), component);
                } else {
                    // escape quotes, https://github.com/DiscordSRV/DiscordSRV/issues/754
                    playerMessage = playerMessage.replace("\"", "\\\"");
                    String json = Format.convertPlainTextToJson(playerMessage, true);
                    int hash = (playerMessage.replaceAll("(§([a-z0-9]))", "")).hashCode();
                    String finalJSON = Format.formatModerationGUI(json, player.getPlayer(), "Discord", chatChannel.getName(), hash);
                    PacketContainer packet = Format.createPacketPlayOutChat(finalJSON);
                    Format.sendPacketPlayOutChat(player.getPlayer(), packet);
                }
            }

            PlayerUtil.notifyPlayersOfMentions(player ->
                            playersToNotify.stream()
                                    .map(MineverseChatPlayer::getPlayer)
                                    .collect(Collectors.toList())
                                    .contains(player),
                    message
            );
        }
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("VentureChat");
    }
}
