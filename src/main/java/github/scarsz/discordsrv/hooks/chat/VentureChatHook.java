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
import net.dv8tion.jda.api.entities.TextChannel;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.stream.Collectors;

public class VentureChatHook implements ChatHook {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onVentureChatEvent(VentureChatEvent event) {
        // event will fire again when received. Don't want to listen twice on the sending server
        if (event.isBungee()) return;

        // get channel
        ChatChannel chatChannel = event.getChannel();
        String channel = chatChannel.getName();

        String username = event.getUsername();
        String nickname = event.getNickname();

        // get plain text message (no JSON)
        String message = event.getChat();

        String userPrimaryGroup = event.getPlayerPrimaryGroup();
        if (userPrimaryGroup.equals("default")) userPrimaryGroup = " ";

        boolean hasGoodGroup = StringUtils.isNotBlank(userPrimaryGroup);

        // capitalize the first letter of the user's primary group to look neater
        if (hasGoodGroup) {
            userPrimaryGroup = userPrimaryGroup.substring(0, 1).toUpperCase() + userPrimaryGroup.substring(1);
        }

        boolean reserializer = DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToDiscord");

        username = DiscordUtil.strip(username);
        if (!reserializer) username = DiscordUtil.escapeMarkdown(username);

        String discordMessage = (hasGoodGroup
                ? LangUtil.Message.CHAT_TO_DISCORD.toString()
                : LangUtil.Message.CHAT_TO_DISCORD_NO_PRIMARY_GROUP.toString())
            .replaceAll("%time%|%date%", TimeUtil.timeStamp())
            .replace("%channelname%", channel != null ? channel.substring(0, 1).toUpperCase() + channel.substring(1) : "")
            .replace("%primarygroup%", userPrimaryGroup)
            .replace("%username%", username);

        String displayName = DiscordUtil.strip(nickname);
        if (!reserializer) displayName = DiscordUtil.escapeMarkdown(displayName);

        discordMessage = discordMessage
                .replace("%displayname%", displayName)
                .replace("%message%", message);

        if (!reserializer) {
            discordMessage = DiscordUtil.strip(discordMessage);
            message = DiscordUtil.strip(message);
        }

        if (DiscordSRV.config().getBoolean("DiscordChatChannelTranslateMentions")) {
            discordMessage = DiscordUtil.convertMentionsFromNames(discordMessage, DiscordSRV.getPlugin().getMainGuild());
        } else {
            discordMessage = discordMessage.replace("@", "@\u200B"); // zero-width space
            message = message.replace("@", "@\u200B"); // zero-width space
        }

        if (reserializer) {
            discordMessage = DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacy().deserialize(discordMessage));
            message = DiscordSerializer.INSTANCE.serialize(LegacyComponentSerializer.legacy().deserialize(message));
        }

        TextChannel textChannel = channel == null ? DiscordSRV.getPlugin().getMainTextChannel()
                : DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);
        if (!DiscordSRV.config().getBoolean("Experiment_WebhookChatMessageDelivery")) {
            DiscordUtil.sendMessage(textChannel, discordMessage);
        } else {
            MineverseChatPlayer chatPlayer = event.getMineverseChatPlayer();
            String webhookUsername = DiscordSRV.config().getString("Experiment_WebhookChatMessageUsernameFormat")
                    .replaceAll("(?:%displayname%)|(?:%username%)", DiscordUtil.strip(event.getUsername()));
            webhookUsername = PlaceholderUtil.replacePlaceholders(webhookUsername);
            webhookUsername = DiscordUtil.strip(webhookUsername);

            WebhookUtil.deliverMessage(textChannel, webhookUsername, DiscordSRV.getPlugin().getEmbedAvatarUrl(username, chatPlayer.getUUID()), message, null);
        }
    }

    @Override
    public void broadcastMessageToChannel(String channel, String message) {
        if (channel.equalsIgnoreCase("global")) channel = "Global";
        ChatChannel chatChannel = ChatChannel.getChannel(channel); // case in-sensitive by default(?)

        if (chatChannel == null) {
            DiscordSRV.debug("Attempted to broadcast message to channel \"" + channel + "\" but got null channel info; aborting message");
            return;
        }

        // filter chat if bad words filter is on for channel and player
        String msg = message;
        if (chatChannel.isFiltered()) msg = Format.FilterChat(msg);

        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelcolor%", ChatColor.valueOf(chatChannel.getColor().toUpperCase()).toString())
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", chatChannel.getAlias())
                .replace("%message%", msg);

        if (chatChannel.getBungee()) {
            if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft")) {
                plainMessage = DiscordSerializer.INSTANCE.serialize(MinecraftSerializer.INSTANCE.serialize(plainMessage));
            }
            MineverseChat.sendDiscordSRVPluginMessage(channel, plainMessage);
        } else {
            List<MineverseChatPlayer> playersToNotify = MineverseChat.onlinePlayers.stream().filter(p -> p.getListening().contains(chatChannel.getName())).collect(Collectors.toList());

            for (MineverseChatPlayer player : playersToNotify) {
                if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_ToMinecraft")) {
                    TextAdapter.sendComponent(player.getPlayer(), MinecraftSerializer.INSTANCE.serialize(plainMessage));
                } else {
                    String json = Format.convertPlainTextToJson(plainMessage, true);
                    int hash = (plainMessage.replaceAll("(§([a-z0-9]))", "")).hashCode();
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
