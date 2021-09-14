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

package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
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

public class CommandLinked {

    @Command(commandNames = { "linked" },
            helpMessage = "Checks what Discord user your (or someone else's) MC account is linked to",
            permission = "discordsrv.linked"
    )
    public static void execute(CommandSender sender, String[] args) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> executeAsync(sender, args));
    }

    private static void executeAsync(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                MessageUtil.sendMessage(sender, LangUtil.Message.LINKED_NOBODY_FOUND.toString()
                        .replace("%target%", "CONSOLE")
                );
                return;
            }

            String linkedId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(((Player) sender).getUniqueId());
            boolean hasLinkedAccount = linkedId != null;

            if (hasLinkedAccount) {
                Member member = DiscordUtil.getMemberById(linkedId);
                String name = member != null ? member.getEffectiveName() : "Discord ID " + linkedId;

                MessageUtil.sendMessage(sender, LangUtil.Message.LINKED_SUCCESS.toString().replace("%name%", name));
            } else {
                MessageUtil.sendMessage(sender, LangUtil.Message.LINK_FAIL_NOT_ASSOCIATED_WITH_AN_ACCOUNT.toString());
            }
        } else {
            if (!sender.hasPermission("discordsrv.linked.others")) {
                MessageUtil.sendMessage(sender, LangUtil.Message.NO_PERMISSION.toString());
                return;
            }

            String target = args[0];
            String joinedTarget = String.join(" ", args);

            if (args.length == 1 && target.length() == 32 || target.length() == 36) {
                // target is UUID
                notifyInterpret(sender, "UUID");
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(target));
                notifyPlayer(sender, player);
                notifyDiscord(sender, DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId()));
                return;
            } else if (args.length == 1 && DiscordUtil.getUserById(target) != null ||
                    (StringUtils.isNumeric(target) && target.length() >= 17 && target.length() <= 20)) {
                // target is a Discord ID
                notifyInterpret(sender, "Discord ID");
                UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(target);
                notifyPlayer(sender, uuid != null ? Bukkit.getOfflinePlayer(uuid) : null);
                notifyDiscord(sender, target);
                return;
            } else {
                if (args.length == 1 && target.length() >= 3 && target.length() <= 16) {
                    // target is probably a Minecraft player name
                    OfflinePlayer player;

                    player = Bukkit.getOnlinePlayers().stream()
                            .filter(p -> p.getName().equalsIgnoreCase(target))
                            .findFirst().orElse(null);

                    if (player == null) {
                        player = Arrays.stream(Bukkit.getOfflinePlayers())
                                .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(target))
                                .findFirst().orElse(null);
                    }

                    if (player == null) {
                        //noinspection deprecation
                        player = Bukkit.getOfflinePlayer(target);
                        if (player.getName() == null) {
                            // player doesn't actually exist
                            player = null;
                        }
                    }

                    if (player != null) {
                        // found them
                        notifyInterpret(sender, "Minecraft player");
                        notifyPlayer(sender, player);
                        notifyDiscord(sender, DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId()));
                        return;
                    }
                }

                if (joinedTarget.contains("#") || (joinedTarget.length() >= 2 && joinedTarget.length() <= 32 + 5)) {
                    // target is a discord name... probably.
                    String targetUsername = joinedTarget.contains("#") ? joinedTarget.split("#")[0] : joinedTarget;
                    String discriminator = joinedTarget.contains("#") ? joinedTarget.split("#")[1] : "";

                    Set<User> matches = DiscordUtil.getJda().getGuilds().stream()
                            .flatMap(guild -> guild.getMembers().stream())
                            .filter(member -> member.getUser().getName().equalsIgnoreCase(targetUsername)
                                    || (member.getNickname() != null && member.getNickname().equalsIgnoreCase(targetUsername)))
                            .filter(member -> member.getUser().getDiscriminator().contains(discriminator))
                            .map(Member::getUser)
                            .collect(Collectors.toSet());

                    if (matches.size() >= 1) {
                        notifyInterpret(sender, "Discord name");

                        matches.stream().limit(5).forEach(user -> {
                            UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(user.getId());
                            notifyPlayer(sender, uuid != null ? Bukkit.getOfflinePlayer(uuid) : null);
                            notifyDiscord(sender, user.getId());
                        });

                        int remaining = matches.size() - 5;
                        if (remaining >= 1) {
                            MessageUtil.sendMessage(sender, String.format("%s+%s%d%s more result%s...",
                                    ChatColor.AQUA, ChatColor.WHITE, remaining, ChatColor.AQUA,
                                    remaining > 1 ? "s" : "")
                            );
                        }
                        return;
                    }
                }
            }

            // no matches at all found
            MessageUtil.sendMessage(sender, LangUtil.Message.LINKED_NOBODY_FOUND.toString().replace("%target%", joinedTarget));
        }
    }

    static void notifyInterpret(CommandSender sender, String type) {
        MessageUtil.sendMessage(sender, String.format("%sInterpreted target as %s%s",
                ChatColor.AQUA, ChatColor.WHITE, type)
        );
    }

    static void notifyPlayer(CommandSender sender, OfflinePlayer player) {
        MessageUtil.sendMessage(sender, String.format("%s-%s Player: %s%s",
                ChatColor.WHITE, ChatColor.AQUA, ChatColor.WHITE, PrettyUtil.beautifyNickname(player))
        );
    }

    static void notifyDiscord(CommandSender sender, String discordId) {
        User user = DiscordUtil.getUserById(discordId);
        String discordInfo = (user != null ? " (" + user.getName() + "#" + user.getDiscriminator() + ")" : "") + " " + discordId;
        MessageUtil.sendMessage(sender, String.format("%s-%s Discord: %s%s%s",
                ChatColor.WHITE, ChatColor.AQUA, ChatColor.WHITE, PrettyUtil.beautify(user), discordInfo)
        );
    }

}
