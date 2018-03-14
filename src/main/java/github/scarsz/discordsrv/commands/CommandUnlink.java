/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2018 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.PrettyUtil;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class CommandUnlink {

    @Command(commandNames = { "unlink", "clearlinked" },
            helpMessage = "Unlinks your Minecraft account from your Discord account",
            permission = "discordsrv.unlink"
    )
    public static void execute(Player sender, String[] args) {
        if (args.length == 0) {
            String linkedId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(sender.getUniqueId());
            boolean hadLinkedAccount = linkedId != null;

            DiscordSRV.getPlugin().getAccountLinkManager().unlink(sender.getUniqueId());

            if (hadLinkedAccount) {
                Member member = DiscordUtil.getMemberById(linkedId);
                String name = member != null ? member.getEffectiveName() : "Discord ID " + linkedId;

                sender.sendMessage(ChatColor.AQUA + LangUtil.InternalMessage.UNLINK_SUCCESS.toString()
                        .replace("{name}", name)
                );
            } else {
                sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.LINK_FAIL_NOT_ASSOCIATED_WITH_AN_ACCOUNT.toString());
            }
        } else {
            if (!sender.hasPermission("discordsrv.unlink.others")) {
                sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.NO_PERMISSION.toString());
                return;
            }

            String target = args[0];

            if (DiscordUtil.getUserById(target) != null) { // discord id given
                User targetUser = DiscordUtil.getUserById(target);
                UUID targetUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(target);
                OfflinePlayer targetPlayer = Bukkit.getPlayer(targetUuid);

                if (targetUuid != null) {
                    DiscordSRV.getPlugin().getAccountLinkManager().unlink(targetUuid);
                    sender.sendMessage(ChatColor.AQUA + PrettyUtil.beautify(targetUser) + " <✗> " + PrettyUtil.beautify(targetPlayer));
                } else {
                    sender.sendMessage(ChatColor.RED + PrettyUtil.beautify(targetUser) + " <?>");
                }
            } else if (target.length() == 32 || target.length() == 36) { // uuid given
                UUID targetUuid = UUID.fromString(target);
                OfflinePlayer targetPlayer = Bukkit.getPlayer(targetUuid);
                User targetUser = DiscordUtil.getUserById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(targetUuid));

                if (targetUser != null) {
                    DiscordSRV.getPlugin().getAccountLinkManager().unlink(targetUuid);
                    sender.sendMessage(ChatColor.AQUA + PrettyUtil.beautify(targetUser) + " <✗> " + PrettyUtil.beautify(targetPlayer));
                } else {
                    sender.sendMessage(ChatColor.RED + PrettyUtil.beautify(targetPlayer) + " <?>");
                }
            } else if (Bukkit.getPlayerExact(target) != null) { // player name given
                OfflinePlayer targetPlayer = Bukkit.getPlayerExact(target);
                UUID targetUuid = targetPlayer.getUniqueId();
                User targetUser = DiscordUtil.getUserById(DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(targetUuid));

                if (targetUser != null) {
                    DiscordSRV.getPlugin().getAccountLinkManager().unlink(targetUuid);
                    sender.sendMessage(ChatColor.AQUA + PrettyUtil.beautify(targetUser) + " <✗> " + PrettyUtil.beautify(targetPlayer));
                } else {
                    sender.sendMessage(ChatColor.RED + PrettyUtil.beautify(targetPlayer) + " <?>");
                }
            } else { // discord name given?
                List<User> matchingUsers = DiscordUtil.getJda().getUsersByName(String.join(" ", args), true);

                if (matchingUsers.size() == 0) {
                    sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.LINKED_NOBODY_FOUND.toString()
                            .replace("{target}", target)
                    );
                    return;
                }

                for (User targetUser : matchingUsers) {
                    UUID targetUuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(targetUser.getId());
                    OfflinePlayer targetPlayer = Bukkit.getPlayer(targetUuid);

                    if (targetUuid != null) {
                        DiscordSRV.getPlugin().getAccountLinkManager().unlink(targetUuid);
                        sender.sendMessage(ChatColor.AQUA + PrettyUtil.beautify(targetUser) + " <✗> " + PrettyUtil.beautify(targetPlayer));
                    } else {
                        sender.sendMessage(ChatColor.RED + PrettyUtil.beautify(targetUser) + " <?>");
                    }
                }
            }
        }
    }

}
