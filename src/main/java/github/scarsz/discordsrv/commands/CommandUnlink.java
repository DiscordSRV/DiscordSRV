package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.LangUtil;
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

import static github.scarsz.discordsrv.commands.CommandLinked.*;

public class CommandUnlink {

    @Command(commandNames = { "unlink", "clearlinked" },
            helpMessage = "Unlinks your Minecraft account from your Discord account",
            permission = "discordsrv.unlink"
    )
    public static void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + LangUtil.InternalMessage.NO_UNLINK_TARGET_SPECIFIED.toString());
                return;
            }

            Player player = (Player) sender;
            String linkedId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
            boolean hasLinkedAccount = linkedId != null;

            if (hasLinkedAccount) {
                Member member = DiscordUtil.getMemberById(linkedId);
                String name = member != null ? member.getEffectiveName() : "Discord ID " + linkedId;

                DiscordSRV.getPlugin().getAccountLinkManager().unlink(player.getUniqueId());
                sender.sendMessage(ChatColor.AQUA + LangUtil.InternalMessage.UNLINK_SUCCESS.toString()
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

            if (target.length() == 32 || target.length() == 36 && args.length == 1) {
                // target is UUID
                notifyInterpret(sender, "UUID");
                OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(target));
                notifyPlayer(sender, player);
                String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
                notifyDiscord(sender, discordId);
                if (discordId != null) {
                    DiscordSRV.getPlugin().getAccountLinkManager().unlink(discordId);
                    notifyUnlinked(sender);
                }
                return;
            } else if (args.length == 1 && DiscordUtil.getUserById(target) != null ||
                    (StringUtils.isNumeric(target) && target.length() >= 17 && target.length() <= 20)) {
                // target is a Discord ID
                notifyInterpret(sender, "Discord ID");
                UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(target);
                notifyPlayer(sender, uuid != null ? Bukkit.getOfflinePlayer(uuid) : null);
                notifyDiscord(sender, target);
                if (uuid != null) {
                    DiscordSRV.getPlugin().getAccountLinkManager().unlink(uuid);
                    notifyUnlinked(sender);
                }
                return;
            } else {
                if (args.length == 1 && target.length() >= 3 && target.length() <= 16) {
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

                        DiscordSRV.getPlugin().getAccountLinkManager().unlink(player.getUniqueId());
                        notifyUnlinked(sender);
                        return;
                    }
                }

                if (joinedTarget.contains("#") || (joinedTarget.length() >= 2 && joinedTarget.length() <= 32 + 5)) {
                    // target is a discord name... probably.
                    String targetUsername = joinedTarget.contains("#") ? joinedTarget.split("#")[0] : joinedTarget;
                    String discriminator = joinedTarget.contains("#") ? joinedTarget.split("#")[1] : "";

                    Set<User> matches = DiscordSRV.getPlugin().getMainGuild().getMembers().stream()
                            .filter(member -> member.getUser().getName().equalsIgnoreCase(targetUsername)
                                    || (member.getNickname() != null && member.getNickname().equalsIgnoreCase(targetUsername)))
                            .filter(member -> member.getUser().getDiscriminator().contains(discriminator))
                            .map(Member::getUser)
                            .collect(Collectors.toSet());

                    if (matches.size() != 0) {
                        notifyInterpret(sender, "Discord name");

                        if (matches.size() == 1) {
                            User user = matches.iterator().next();
                            notifyDiscord(sender, user.getId());
                            UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(user.getId());
                            if (uuid != null) {
                                notifyPlayer(sender, Bukkit.getOfflinePlayer(uuid));
                                DiscordSRV.getPlugin().getAccountLinkManager().unlink(user.getId());
                                notifyUnlinked(sender);
                            } else {
                                notifyPlayer(sender, null);
                            }
                        } else {
                            matches.stream().limit(5).forEach(user -> {
                                UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(user.getId());
                                notifyPlayer(sender, uuid != null ? Bukkit.getOfflinePlayer(uuid) : null);
                                notifyDiscord(sender, user.getId());
                            });

                            int remaining = matches.size() - 5;
                            if (remaining >= 1) {
                                sender.sendMessage(String.format("%s+%s%d%s more result%s...",
                                        ChatColor.AQUA, ChatColor.WHITE, remaining, ChatColor.AQUA,
                                        remaining > 1 ? "s" : "")
                                );
                            }

                            sender.sendMessage(ChatColor.AQUA + "Be more specific.");
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

    private static void notifyUnlinked(CommandSender sender) {
        sender.sendMessage(ChatColor.WHITE + "- " + ChatColor.AQUA + " Unlinked ✓");
    }

}
