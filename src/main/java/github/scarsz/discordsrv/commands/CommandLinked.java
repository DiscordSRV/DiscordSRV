/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2019 Austin "Scarsz" Shapiro
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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class CommandLinked {

    @Command(commandNames = { "linked" },
            helpMessage = "Checks what Discord user your (or someone else's) MC account is linked to",
            permission = "discordsrv.linked"
    )
    public static void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.LINKED_NOBODY_FOUND.toString()
                        .replace("{target}", "CONSOLE")
                );
                return;
            }

            String linkedId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(((Player) sender).getUniqueId());
            boolean hasLinkedAccount = linkedId != null;

            if (hasLinkedAccount) {
                Member member = DiscordUtil.getMemberById(linkedId);
                String name = member != null ? member.getEffectiveName() : "Discord ID " + linkedId;

                sender.sendMessage(ChatColor.AQUA + LangUtil.InternalMessage.LINKED_SUCCESS.toString()
                        .replace("{name}", name)
                );
            } else {
                sender.sendMessage(ChatColor.AQUA + LangUtil.InternalMessage.LINK_FAIL_NOT_ASSOCIATED_WITH_AN_ACCOUNT.toString());
            }
        } else {
            if (!sender.hasPermission("discordsrv.linked.others")) {
                sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.NO_PERMISSION.toString());
                return;
            }

            String target = args[0];
            String joinedTarget = String.join(" ", args);

            if (target.length() == 32 || target.length() == 36) {
                // target is UUID
                notifyInterpret(sender, "UUID");
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(target));
                notifyPlayer(sender, player);
                notifyDiscord(sender, DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId()));
                return;
            } else if (DiscordUtil.getUserById(target) != null ||
                    (StringUtils.isNumeric(target) && target.length() >= 17 && target.length() <= 20)) {
                // target is a Discord ID
                notifyInterpret(sender, "Discord ID");
                UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(target);
                notifyPlayer(sender, uuid != null ? Bukkit.getOfflinePlayer(uuid) : null);
                notifyDiscord(sender, target);
                return;
            } else {
                if (target.length() >= 3 && target.length() <= 16) {
                    // target is probably a Minecraft player name
                    OfflinePlayer player = Arrays.stream(Bukkit.getOfflinePlayers())
                            .filter(OfflinePlayer::hasPlayedBefore)
                            .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(target))
                            .findFirst().orElse(null);

                    if (player != null) {
                        // found them
                        notifyInterpret(sender, "Minecraft player");
                        notifyPlayer(sender, player);
                        notifyDiscord(sender, DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId()));
                        return;
                    }
                }

                if (target.contains("#") || (joinedTarget.length() >= 2 && target.length() <= 32 + 5)) {
                    // target is a discord name... probably.
                    String discriminator = target.contains("#") ? target.split("#")[1] : "";

                    Set<Member> matches = DiscordUtil.getJda().getGuilds().stream()
                            .flatMap(guild -> guild.getMembers().stream())
                            .filter(member -> member.getUser().getName().equalsIgnoreCase(target)
                                    || (member.getNickname() != null && member.getNickname().equalsIgnoreCase(target)))
                            .filter(member -> member.getUser().getDiscriminator().contains(discriminator))
                            .collect(Collectors.toSet());

                    if (matches.size() >= 1) {
                        notifyInterpret(sender, "Discord name");

                        matches.stream().limit(5).forEach(member -> {
                            UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(member.getUser().getId());
                            notifyPlayer(sender, uuid != null ? Bukkit.getOfflinePlayer(uuid) : null);
                            notifyDiscord(sender, member.getUser().getId());
                        });

                        int remaining = matches.size() - 5;
                        if (remaining >= 1) {
                            sender.sendMessage(String.format("%s+%s%d%s more result%s...",
                                    ChatColor.AQUA, ChatColor.WHITE, remaining, ChatColor.AQUA,
                                    remaining > 1 ? "s" : "")
                            );
                        }

                        return;
                    }
                }
            }

            // no matches at all found
            sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.LINKED_NOBODY_FOUND.toString()
                    .replace("{target}", joinedTarget)
            );
        }
    }

    private static void notifyInterpret(CommandSender sender, String type) {
        sender.sendMessage(String.format("%sInterpreted target as %s%s",
                ChatColor.AQUA, ChatColor.WHITE, type)
        );
    }

    private static void notifyPlayer(CommandSender sender, OfflinePlayer player) {
        sender.sendMessage(String.format("%s-%s Player: %s%s",
                ChatColor.WHITE, ChatColor.AQUA, ChatColor.WHITE, PrettyUtil.beautify(player))
        );
    }

    private static void notifyDiscord(CommandSender sender, String discordId) {
        User user = DiscordUtil.getUserById(discordId);
        String discordInfo = user != null ? " (" + user.getName() + "#" + user.getDiscriminator() + ")" : "";
        sender.sendMessage(String.format("%s-%s Discord: %s%s%s",
                ChatColor.WHITE, ChatColor.AQUA, ChatColor.WHITE, PrettyUtil.beautify(user), discordInfo)
        );
    }

}
