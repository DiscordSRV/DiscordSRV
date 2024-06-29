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
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.UpdatedVentureChatMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.UpdatedVentureChatMessagePreProcessEvent;
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
import venture.Aust1n46.chat.service.FormatService;
import venture.Aust1n46.chat.service.PlayerApiService;

public class UpdatedVentureChatHook implements ChatHook {
	@Inject
	private PluginMessageController pluginMessageController;
	@Inject
	private FormatService formatService;
	@Inject
	private ConfigService configService;
	@Inject
	private PlayerApiService playerApiService;

	private Plugin plugin;

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onVentureChat(final VentureChatEvent event) {
		if (DiscordSRV.config().getBoolean("VentureChatBungee")) {
			// event will fire again when received. We don't want to process it twice for the sending server
			if (event.isBungee()) {
				DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received a VentureChat event that is to be sent to BungeeCord; ignoring due to VentureChatBungee being enabled");
				return;
			}
		} else {
			// since experimental bungee compatibility is disabled, we don't care about the second event fired
			if (!event.isBungee() && event.getChannel().isBungeeEnabled()) {
				DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received a VentureChat event from BungeeCord; ignoring due to VentureChatBungee being disabled");
				return;
			}
		}
		final VentureChatPlayer ventureChatPlayer = event.getVentureChatPlayer();
		DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, "Received a VentureChatEvent (player: " + (ventureChatPlayer != null ? ventureChatPlayer.getName() : "null") + ")");
		if (ventureChatPlayer != null) {
			// these events are never cancelled
			DiscordSRV.getPlugin().processChatMessage(ventureChatPlayer.getPlayer(), event.getChat(), event.getChannel().getName(), false, event);
		} else {
			processChatMessageWithNoPlayer(event);
		}
	}

	/**
	 * Copied from {@link DiscordSRV#processChatMessage} for supporting messages
	 * with no player
	 */
	// TODO Remove experimental BungeeCord feature and delete this
	// TODO Or refactor this logic into something neater inside of the main logic
	@SuppressWarnings("deprecation")
	private void processChatMessageWithNoPlayer(final VentureChatEvent event) {
		final VentureChatPlayer chatPlayer = event.getVentureChatPlayer();
		final ChatChannel chatChannel = event.getChannel();
		final boolean bungeeReceive = !event.isBungee() && chatChannel.isBungeeEnabled();
		String message = event.getChat();
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

		UpdatedVentureChatMessagePreProcessEvent preEvent = DiscordSRV.api.callEvent(new UpdatedVentureChatMessagePreProcessEvent(channel, message, event));
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

		UpdatedVentureChatMessagePostProcessEvent postEvent = DiscordSRV.api
				.callEvent(new UpdatedVentureChatMessagePostProcessEvent(channel, discordMessage, event, preEvent.isCancelled()));
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
	public void broadcastMessageToChannel(final String channel, final Component component) {
		final ChatChannel chatChannel = configService.getChannel(channel); // case in-sensitive
		if (chatChannel == null) {
			DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT,
					"Attempted to broadcast message to channel \"" + channel + "\" but the channel doesn't exist (returned null); aborting message send");
			return;
		}
		final String message = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
				.replace("%channelname%", chatChannel.getName())
				.replace("%channelnickname%", chatChannel.getAlias())
				.replace("%message%", MessageUtil.toLegacy(component))
				.replace("%channelcolor%", chatChannel.getColor());
		final String playerMessage = chatChannel.isFiltered() ? formatService.filterChat(message) : message;
		if (DiscordSRV.config().getBoolean("VentureChatBungee") && chatChannel.isBungeeEnabled()) {
			pluginMessageController.sendDiscordSRVPluginMessage(chatChannel.getName(), playerMessage);
			DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Sent a message to VentureChat via BungeeCord (channel: " + chatChannel.getName() + ")");
		} else {
			formatService.createAndSendExternalChatMessage(playerMessage, chatChannel.getName(), "Discord");
			PlayerUtil.notifyPlayersOfMentions(player -> playerApiService.getOnlineMineverseChatPlayers()
					.stream()
					.filter(vcp -> configService.isListening(vcp, chatChannel.getName()))
					.map(VentureChatPlayer::getPlayer)
					.toList().contains(player), message);
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
