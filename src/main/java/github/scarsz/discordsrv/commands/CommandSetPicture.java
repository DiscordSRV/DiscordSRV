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

package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.apache.commons.io.FileUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class CommandSetPicture {

    @Command(commandNames = { "setpicture" },
            helpMessage = "Sets the picture of the bot on Discord",
            permission = "discordsrv.setpicture",
            usageExample = "setpicture http://i.imgur.com/kU90G2g.jpg"
    )
    public static void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "No URL given");
        } else {
            File pictureFile = new File(DiscordSRV.getPlugin().getDataFolder(), "picture.jpg");
            try {
                FileUtils.copyURLToFile(new URL(args[0]), pictureFile);
                DiscordUtil.setAvatarBlocking(pictureFile);
                sender.sendMessage(ChatColor.AQUA + "✓");
            } catch (IOException | RuntimeException e) {
                sender.sendMessage(ChatColor.RED + "✗: " + e.getMessage());
            }
            pictureFile.delete();
        }
    }

}
