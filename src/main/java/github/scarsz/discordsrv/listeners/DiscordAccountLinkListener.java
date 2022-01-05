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

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.DiscordPrivateMessageReceivedEvent;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;

public class DiscordAccountLinkListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // don't process messages sent by bot or webhook
        if (event.getAuthor().isBot() || event.getMessage().isWebhookMessage()) return;
        String option = DiscordSRV.config().getString("LinkAccountChannel");
        if (StringUtils.isNotBlank(option.replace("0", ""))) {
            if (!(event.getChannel() instanceof PrivateChannel)) return;
        } else {
            if (event.getChannel().getId() != option) return;
            if (DiscordSRV.config().getBooleanElse("LinkAccountDeleteCode", false)) event.getMessage().delete().queue();
        }
        String reply = DiscordSRV.getPlugin().getAccountLinkManager().process(event.getMessage().getContentRaw(), event.getAuthor().getId());
        if (reply != null) event.getChannel().sendMessage(reply).queue();
    }

    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        DiscordSRV.api.callEvent(new DiscordPrivateMessageReceivedEvent(event));
    }


    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        // add linked role and nickname back to people when they rejoin the server
        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getUser().getId());
        if (uuid != null) {
            Role roleToAdd = DiscordUtil.resolveRole(DiscordSRV.config().getString("MinecraftDiscordAccountLinkedRoleNameToAddUserTo"));
            if (roleToAdd != null) DiscordUtil.addRoleToMember(event.getMember(), roleToAdd);
            else DiscordSRV.debug(Debug.GROUP_SYNC, "Couldn't add user to null role");

            if (DiscordSRV.config().getBoolean("NicknameSynchronizationEnabled")) {
                OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                DiscordSRV.getPlugin().getNicknameUpdater().setNickname(event.getMember(), player);
            }
        }
    }

}
