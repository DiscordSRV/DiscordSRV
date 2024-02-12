/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.hooks.chat;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.events.PacketContainer;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.VentureChatMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.VentureChatMessagePreProcessEvent;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import github.scarsz.discordsrv.util.TimeUtil;
import github.scarsz.discordsrv.util.WebhookUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.adventure.text.Component;
import shaded.com.google.inject.Inject;
import venture.Aust1n46.chat.api.events.VentureChatEvent;
import venture.Aust1n46.chat.controllers.PluginMessageController;
import venture.Aust1n46.chat.initiators.application.VentureChat;
import venture.Aust1n46.chat.model.ChatChannel;
import venture.Aust1n46.chat.model.VentureChatPlayer;
import venture.Aust1n46.chat.service.ConfigService;
import venture.Aust1n46.chat.service.VentureChatFormatService;
import venture.Aust1n46.chat.service.VentureChatPlayerApiService;

public class VentureChatHook implements ChatHook {
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private VentureChatFormatService formatService;
	@Inject
	private ConfigService configService;
	@Inject
	private VentureChatPlayerApiService apiService;

	private Plugin plugin;

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onVentureChat(VentureChatEvent event) {
		boolean shouldUseBungee = DiscordSRV.config().getBoolean("VentureChatBungee");

		ChatChannel chatChannel = event.getChannel();
		if (chatChannel == null) {
			// uh oh, ok then
			DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received VentureChatEvent with a null channel");
			return;
		}

		boolean bungeeSend = event.isBungee();
		boolean bungeeReceive = !bungeeSend && chatChannel.getBungee();

		if (shouldUseBungee) {
			// event will fire again when received. don't want to process it twice for the
			// sending server
			if (bungeeSend) {
				DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received a VentureChat event that it to be sent to BungeeCord, ignoring due to VentureChatBungee being enabled");
				return;
			}
		} else {
			// since bungee compatability is disabled, we don't care about messages that we
			// receive
			if (bungeeReceive) {
				DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received a VentureChat event from BungeeCord, ignoring due to VentureChatBungee being disabled");
				return;
			}
		}

		String message = event.getChat();
		VentureChatPlayer chatPlayer = event.getVentureChatPlayer();
		DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received a VentureChatEvent (player: " + (chatPlayer != null ? chatPlayer.getName() : "null") + ")");

		if (chatPlayer != null) {
			Player player = chatPlayer.getPlayer();
			if (player != null) {
				// these events are never cancelled
				DiscordSRV.getPlugin().processChatMessage(player, message, chatChannel.getName(), false, event);
				return;
			}
		}

		if (!shouldUseBungee) {
			DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received a VentureChat message with a null MineverseChatPlayer or Player (and BungeeCord is disabled)");
			return;
		}

		// Below is copied from DiscordSRV#processChatMessage for supporting messages
		// with no player

		DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD,
				"Processing VentureChat message without a Player object" + (bungeeReceive ? " (a BungeeCord receive)" : " (not a BungeeCord receive)"));
		if (!DiscordSRV.config().getBoolean("DiscordChatChannelMinecraftToDiscord")) {
			DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD,
					"A VentureChat message was received but it was not delivered to Discord because DiscordChatChannelMinecraftToDiscord is false");
			return;
		}

		String prefix = DiscordSRV.config().getString("DiscordChatChannelPrefixRequiredToProcessMessage");
		if (!MessageUtil.strip(message).startsWith(prefix)) {
			DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "A VentureChat message was received but it was not delivered to Discord because the message didn't start with \"" + prefix
					+ "\" (DiscordChatChannelPrefixRequiredToProcessMessage): \"" + message + "\"");
			return;
		}

		String channel = chatChannel.getName();

		VentureChatMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new VentureChatMessagePreProcessEvent(channel, message, event));
		if (preEvent.isCancelled()) {
			DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "VentureChatMessagePreProcessEvent was cancelled, message send aborted");
			return;
		}
		channel = preEvent.getChannel(); // update channel from event in case any listeners modified it
		message = preEvent.getMessage(); // update message from event in case any listeners modified it

		String userPrimaryGroup = event.getPlayerPrimaryGroup();
		if (userPrimaryGroup.equals("default"))
			userPrimaryGroup = "";

		boolean hasGoodGroup = StringUtils.isNotBlank(userPrimaryGroup);

		// capitalize the first letter of the user's primary group to look neater
		if (hasGoodGroup)
			userPrimaryGroup = userPrimaryGroup.substring(0, 1).toUpperCase() + userPrimaryGroup.substring(1);

		boolean reserializer = DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToDiscord");

		String username = event.getUsername();
		String formatUsername = username;
		if (!reserializer)
			formatUsername = DiscordUtil.escapeMarkdown(username);

		String discordMessage = (hasGoodGroup ? LangUtil.Message.CHAT_TO_DISCORD.toString() : LangUtil.Message.CHAT_TO_DISCORD_NO_PRIMARY_GROUP.toString())
				.replaceAll("%time%|%date%", TimeUtil.timeStamp()).replace("%channelname%", channel != null ? channel.substring(0, 1).toUpperCase() + channel.substring(1) : "")
				.replace("%primarygroup%", userPrimaryGroup).replace("%username%", formatUsername);
		discordMessage = PlaceholderUtil.replacePlaceholdersToDiscord(discordMessage);

		String displayName = MessageUtil.strip(username);
		if (reserializer) {
			message = MessageUtil.reserializeToDiscord(MessageUtil.toComponent(message));
		} else {
			displayName = DiscordUtil.escapeMarkdown(displayName);
		}

		discordMessage = discordMessage.replace("%displayname%", displayName).replace("%message%", message);

		if (!reserializer)
			discordMessage = MessageUtil.strip(discordMessage);

		if (DiscordSRV.config().getBoolean("DiscordChatChannelTranslateMentions")) {
			discordMessage = DiscordUtil.convertMentionsFromNames(discordMessage, DiscordSRV.getPlugin().getMainGuild());
		} else {
			discordMessage = discordMessage.replace("@", "@\u200B"); // zero-width space
			message = message.replace("@", "@\u200B"); // zero-width space
		}

		VentureChatMessagePostProcessEvent postEvent = DiscordSRV.api.callEvent(new VentureChatMessagePostProcessEvent(channel, discordMessage, event, preEvent.isCancelled()));
		if (postEvent.isCancelled()) {
			DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "VentureChatMessagePostProcessEvent was cancelled, message send aborted");
			return;
		}
		channel = postEvent.getChannel(); // update channel from event in case any listeners modified it
		discordMessage = postEvent.getProcessedMessage(); // update message from event in case any listeners modified it

		if (!DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageDelivery")) {
			if (channel == null) {
				DiscordUtil.sendMessage(DiscordSRV.getPlugin().getOptionalTextChannel("global"), discordMessage);
			} else {
				DiscordUtil.sendMessage(DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel), discordMessage);
			}
		} else {
			if (channel == null)
				channel = DiscordSRV.getPlugin().getOptionalChannel("global");

			TextChannel destinationChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);
			if (destinationChannel == null) {
				DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Failed to find Discord channel to forward message from game channel " + channel);
				return;
			}

			if (!DiscordUtil.checkPermission(destinationChannel.getGuild(), Permission.MANAGE_WEBHOOKS)) {
				DiscordSRV.error("Couldn't deliver chat message as webhook because the bot lacks the \"Manage Webhooks\" permission.");
				return;
			}

			message = PlaceholderUtil.replacePlaceholdersToDiscord(message);
			if (!reserializer)
				message = MessageUtil.strip(message);

			if (DiscordSRV.config().getBoolean("DiscordChatChannelTranslateMentions"))
				message = DiscordUtil.convertMentionsFromNames(message, DiscordSRV.getPlugin().getMainGuild());

			String webhookUsername = DiscordSRV.config().getString("Experiment_WebhookChatMessageUsernameFormat").replaceAll("(?:%displayname%)|(?:%username%)", username);
			webhookUsername = PlaceholderUtil.replacePlaceholders(webhookUsername);
			webhookUsername = MessageUtil.strip(webhookUsername);

			UUID uuid = chatPlayer != null ? chatPlayer.getUuid() : null;
			OfflinePlayer offlinePlayer = uuid != null ? Bukkit.getOfflinePlayer(uuid) : null;
			if (offlinePlayer != null) {
				String name = chatPlayer.getName() != null ? chatPlayer.getName() : chatPlayer.getName();
				WebhookUtil.deliverMessage(destinationChannel, offlinePlayer, name, message, (Collection<? extends MessageEmbed>) null);
			} else {
				// noinspection ConstantConditions
				WebhookUtil.deliverMessage(destinationChannel, webhookUsername, DiscordSRV.getAvatarUrl(username, uuid), message, (Collection<? extends MessageEmbed>) null);
			}
		}
	}

	@Override
	public void broadcastMessageToChannel(String channel, Component component) {
		ChatChannel chatChannel = configService.getChannel(channel); // case in-sensitive
		if (chatChannel == null) {
			DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT,
					"Attempted to broadcast message to channel \"" + channel + "\" but the channel doesn't exist (returned null); aborting message send");
			return;
		}
		String legacy = MessageUtil.toLegacy(component);

		String channelColor = null;
		try {
			channelColor = ChatColor.valueOf(chatChannel.getColor().toUpperCase()).toString();
		} catch (Exception ignored) {
			// if it has a section sign it's probably a already formatted color
			if (chatChannel.getColor().contains(MessageUtil.LEGACY_SECTION.toString())) {
				channelColor = MessageUtil.translateLegacy(chatChannel.getColor());
			}
		}

		String message = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString().replace("%channelname%", chatChannel.getName()).replace("%channelnickname%", chatChannel.getAlias())
				.replace("%message%", legacy).replace("%channelcolor%", MessageUtil.translateLegacy(channelColor != null ? channelColor : ""));

		if (DiscordSRV.config().getBoolean("VentureChatBungee") && chatChannel.getBungee()) {
			if (chatChannel.isFiltered())
				message = formatService.FilterChat(message);
			String translatedMessage = MessageUtil.toLegacy(MessageUtil.toComponent(message));
			pluginMessageController.sendDiscordSRVPluginMessage(chatChannel.getName(), translatedMessage);
			DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Sent a message to VentureChat via BungeeCord (channel: " + chatChannel.getName() + ")");
		} else {
			List<VentureChatPlayer> playersToNotify = apiService.getOnlineMineverseChatPlayers().stream().filter(p -> p.getListening().contains(chatChannel.getName()))
					.filter(p -> !chatChannel.hasPermission() || p.getPlayer().hasPermission(chatChannel.getPermission())).collect(Collectors.toList());
			for (VentureChatPlayer player : playersToNotify) {
				String playerMessage = (player.isFilter() && chatChannel.isFiltered()) ? formatService.FilterChat(message) : message;

				// escape quotes, https://github.com/DiscordSRV/DiscordSRV/issues/754
				playerMessage = playerMessage.replace("\"", "\\\"");
				String json = formatService.convertPlainTextToJson(playerMessage, true);
				int hash = (playerMessage.replaceAll("(§([a-z0-9]))", "")).hashCode();
				String finalJSON = formatService.formatModerationGUI(json, player.getPlayer(), "Discord", chatChannel.getName(), hash);
				PacketContainer packet = formatService.createPacketPlayOutChat(finalJSON);
				formatService.sendPacketPlayOutChat(player.getPlayer(), packet);
			}

			PlayerUtil.notifyPlayersOfMentions(player -> playersToNotify.stream().map(VentureChatPlayer::getPlayer).collect(Collectors.toList()).contains(player), message);
		}
	}

	@Override
	public Plugin getPlugin() {
		if (plugin == null) {
			plugin = PluginUtil.getPlugin("VentureChat");
		}
		return plugin;
	}

	@Override
	public boolean isEnabled() {
		if (!ChatHook.super.isEnabled()) {
			return false;
		}
		try {
			Class.forName("venture.Aust1n46.chat.api.events.VentureChatEvent");
		} catch (final ClassNotFoundException ignore) {
			return false;
		}
		return true;
	}

	@Override
	public void hook() {
		final VentureChat ventureChat = (VentureChat) getPlugin();
		ventureChat.injectDependencies(this);
	}
}
