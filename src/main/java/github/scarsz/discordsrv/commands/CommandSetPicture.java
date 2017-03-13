package github.scarsz.discordsrv.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.apache.commons.io.FileUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 3/12/2017
 * @at 2:58 PM
 */
public class CommandSetPicture {

    @Command(commandNames = { "setpicture" },
            helpMessage = "Sets the picture of the bot on Discord",
            permission = "discordsrv.setpicture",
            usageExample = "setpicture http://i.imgur.com/kU90G2g.jpg"
    )
    public static void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "No picture URL given");
        } else {
            sender.sendMessage(ChatColor.AQUA + "Downloading picture...");
            File pictureFile = new File(DiscordSRV.getPlugin().getDataFolder(), "picture.jpg");
            try {
                FileUtils.copyURLToFile(new URL(args[0]), pictureFile);
                DiscordUtil.setAvatarBlocking(pictureFile);
                sender.sendMessage(ChatColor.AQUA + "Picture updated successfully");
            } catch (IOException | RuntimeException e) {
                sender.sendMessage("Failed to update picture: " + e.getLocalizedMessage());
            }
            pictureFile.delete();
        }
    }

}
