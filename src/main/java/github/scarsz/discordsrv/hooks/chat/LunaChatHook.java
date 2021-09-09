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

package github.scarsz.discordsrv.hooks.chat;

import com.github.ucchyocean.lc3.LunaChatBukkit;
import com.github.ucchyocean.lc3.bukkit.event.LunaChatBukkitChannelChatEvent;
import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.member.ChannelMemberBukkit;
import com.github.ucchyocean.lc3.member.ChannelMemberPlayer;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import github.scarsz.discordsrv.util.PlayerUtil;
import github.scarsz.discordsrv.util.PluginUtil;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.Plugin;

import java.util.stream.Collectors;

public class LunaChatHook implements ChatHook {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMessage(LunaChatBukkitChannelChatEvent event) {
        // make sure message isn't just blank
        if (StringUtils.isBlank(event.getNgMaskedMessage())) return;

        // get sender player
        Player player = (event.getMember() != null && event.getMember() instanceof ChannelMemberPlayer) ? ((ChannelMemberPlayer) event.getMember()).getPlayer() : null;

        DiscordSRV.getPlugin().processChatMessage(player, event.getNgMaskedMessage(), event.getChannel().getName(), false);
    }

    @Override
    public void broadcastMessageToChannel(String channel, Component message) {
        Channel chatChannel = LunaChatBukkit.getInstance().getLunaChatAPI().getChannel(channel);
        DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Resolved LunaChat channel " + channel + " -> " + chatChannel + (chatChannel != null ? " (" + chatChannel.getName() + ")" : ""));
        if (chatChannel == null) return; // no suitable channel found
        String legacy = MessageUtil.toLegacy(message);

        String plainMessage = LangUtil.Message.CHAT_CHANNEL_MESSAGE.toString()
                .replace("%channelname%", chatChannel.getName())
                .replace("%channelnickname%", (chatChannel.getAlias().equals(""))
                        ? chatChannel.getName() : chatChannel.getAlias())
                .replace("%message%", legacy)
                .replace("%channelcolor%", MessageUtil.toLegacy(MessageUtil.toComponent(MessageUtil.translateLegacy(chatChannel.getColorCode()))));

        String translatedMessage = MessageUtil.translateLegacy(plainMessage);
        chatChannel.chatFromOtherSource("Discord", null, translatedMessage);

        PlayerUtil.notifyPlayersOfMentions(player ->
                        chatChannel.getMembers().stream()
                                .filter(member -> member instanceof ChannelMemberBukkit)
                                .map(member -> ((ChannelMemberBukkit) member).getPlayer())
                                .collect(Collectors.toList())
                                .contains(player),
                legacy);
    }

    @Override
    public Plugin getPlugin() {
        return PluginUtil.getPlugin("LunaChat");
    }

}
