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
import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager;
import github.scarsz.discordsrv.util.LangUtil;
import github.scarsz.discordsrv.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.concurrent.TimeUnit;

public class CommandResync {

    @Command(commandNames = { "resync" },
            helpMessage = "Resynchronizes all groups & roles",
            permission = "discordsrv.resync"
    )
    public static void execute(CommandSender sender, String[] args) {
        if (!DiscordSRV.getPlugin().isGroupRoleSynchronizationEnabled()) {
            MessageUtil.sendMessage(sender, ChatColor.RED + LangUtil.InternalMessage.RESYNC_WHEN_GROUP_SYNC_DISABLED.toString());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () -> {
            MessageUtil.sendMessage(sender, ChatColor.AQUA + "Full group synchronization triggered.");
            long time = System.currentTimeMillis();
            DiscordSRV.getPlugin().getGroupSynchronizationManager().resyncEveryone(GroupSynchronizationManager.SyncCause.MANUAL);
            time = System.currentTimeMillis() - time;
            int seconds = Math.toIntExact(TimeUnit.MILLISECONDS.toSeconds(time));
            MessageUtil.sendMessage(sender, ChatColor.AQUA + "Full group synchronization finished, taking " + seconds + " seconds.");
        });
    }

}
