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
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;

public class CommandBroadcast {

    @Command(commandNames = { "broadcast", "bcast" },
            helpMessage = "Broadcasts a message to the main text channel on Discord",
            permission = "discordsrv.bcast",
            usageExample = "broadcast <#channel?> Hello from the server!"
    )
    public static void execute(CommandSender sender, String[] args) {
        String[] finalArgs;
        TextChannel target = null;

        if (args.length > 0 && args[0].startsWith("#")) {
            String raw = args[0].replace("#", "");
            if (StringUtils.isNumeric(raw)) target = DiscordUtil.getTextChannelById(raw);
            if (target == null) {
                List<TextChannel> mainGuildResults = DiscordSRV.getPlugin().getMainGuild().getTextChannelsByName(raw, true);
                if (mainGuildResults.size() >= 1) {
                    target = mainGuildResults.get(0);
                } else {
                    target = DiscordUtil.getJda().getGuilds().stream()
                            .flatMap(guild -> guild.getTextChannelsByName(raw, true).stream())
                            .filter(Objects::nonNull)
                            .findFirst().orElse(null);
                }
            }
            finalArgs = ArrayUtils.subarray(args, 1, args.length);
        } else {
            target = DiscordSRV.getPlugin().getOptionalTextChannel("broadcasts");
            finalArgs = args;
        }

        if (finalArgs.length == 0) {
            MessageUtil.sendMessage(sender, ChatColor.RED + LangUtil.InternalMessage.NO_MESSAGE_GIVEN_TO_BROADCAST.toString());
        } else {
            String rawMessage = String.join(" ", finalArgs).replace("\\n", "\n");
            rawMessage = PlaceholderUtil.replacePlaceholdersToDiscord(rawMessage);
            if (DiscordSRV.config().getBoolean("DiscordChatChannelTranslateMentions"))
                rawMessage = DiscordUtil.convertMentionsFromNames(rawMessage, DiscordSRV.getPlugin().getMainGuild());
            rawMessage = PlayerUtil.convertTargetSelectors(rawMessage, sender);

            if (DiscordSRV.config().getBoolean("Experiment_MCDiscordReserializer_InBroadcast")) {
                DiscordUtil.queueMessage(
                        target,
                        MessageUtil.reserializeToDiscord(MessageUtil.toComponent(MessageUtil.translateLegacy(rawMessage))),
                        true
                );
            } else {
                DiscordUtil.queueMessage(target, rawMessage, true);
            }
        }
    }

}
