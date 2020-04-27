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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import to.us.gempowered.chat.events.AsyncChatChannelMessageEvent;

public class PlayerChatListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onAsyncPlayerChat(AsyncChatChannelMessageEvent event) {
		Bukkit.getScheduler().runTaskAsynchronously(DiscordSRV.getPlugin(), () ->
		{
			if (event.getPlayer() != null && event.getChannel().getName().equals("Global") || event.getChannel().getName().equals("Roleplay") || event.getChannel().getName().equals("Staff"))
				DiscordSRV.getPlugin().processChatMessage(event.getPlayer(), event.getPrefix() + " " + event.getPlayer().getDisplayName() + " (" + event.getPlayer().getName() + ")" ,event.getPlainTextMessage(), event.getChannel().getName(), event.isCancelled());
		});
	}

}
