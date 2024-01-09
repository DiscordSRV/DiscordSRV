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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordPrivateMessageReceivedEvent;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DiscordAccountLinkListener extends ListenerAdapter {

    @Override
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        // don't process messages sent by the bot
        if (event.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) return;

        DiscordSRV.api.callEvent(new DiscordPrivateMessageReceivedEvent(event));

        // don't link accounts if config option is disabled
        if (!DiscordSRV.config().getBoolean("MinecraftDiscordAccountLinkedUsePM")) return;

        String reply = DiscordSRV.getPlugin().getAccountLinkManager().process(event.getMessage().getContentRaw(), event.getAuthor().getId());
        if (reply != null) event.getMessage().reply(reply).queue();
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        // don't process messages sent by bots
        if (event.getAuthor().isBot()) return;

        // if message is not in the link channel, don't process it
        TextChannel linkChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName("link");
        if (!event.getChannel().equals(linkChannel)) return;

        Message receivedMessage = event.getMessage();
        String reply = DiscordSRV.getPlugin().getAccountLinkManager().process(receivedMessage.getContentRaw(), event.getAuthor().getId());
        if (reply != null) {
            receivedMessage.reply(reply).queue(replyMessage -> {
                // delete the message after a delay if the config option is enabled
                int deleteSeconds = DiscordSRV.config().getIntElse("MinecraftDiscordAccountLinkedMessageDeleteSeconds", 0);
                if (deleteSeconds > 0) {
                    // delete the message after a delay
                    replyMessage.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS);
                    receivedMessage.delete().queueAfter(deleteSeconds, TimeUnit.SECONDS);
                }
            });
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        // add linked role and nickname back to people when they rejoin the server
        Collection<UUID> uuids = DiscordSRV.getPlugin().getAccountLinkManager().getUuids(event.getUser().getId());
        if (uuids != null && !uuids.isEmpty()) {
            Role roleToAdd = DiscordUtil.resolveRole(DiscordSRV.config().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"));
            if (roleToAdd == null || roleToAdd.getGuild().equals(member.getGuild())) {
                if (roleToAdd != null) DiscordUtil.addRoleToMember(member, roleToAdd);
                else DiscordSRV.debug(Debug.GROUP_SYNC, "Couldn't add user to null role");
            } else {
                DiscordSRV.debug(Debug.GROUP_SYNC, "Not adding role to member upon guild join due to the guild being different! (" + roleToAdd.getGuild() + " / " + member.getGuild() + ")");
            }

            if (DiscordSRV.config().getBoolean("NicknameSynchronizationEnabled")) {
                for (UUID uuid : uuids) {
                    OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                    DiscordSRV.getPlugin().getNicknameUpdater().setNickname(member, player);
                }
            }
        }
    }

}
