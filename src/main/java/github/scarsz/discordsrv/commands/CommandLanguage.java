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

import github.scarsz.configuralize.Language;
import github.scarsz.configuralize.Provider;
import github.scarsz.configuralize.Source;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandLanguage {

    @Command(commandNames = { "language", "setlanguage", "lang", "setlang" },
            helpMessage = "Changes the language of DiscordSRV to whatever is specified.",
            permission = "discordsrv.language",
            usageExample = "language japanese"
    )
    public static void execute(CommandSender sender, String[] args) throws IOException {
        Language currentLanguage = DiscordSRV.config().getLanguage();
        String currentLanguageName = StringUtils.capitalize(currentLanguage.getName().toLowerCase());

        Language targetLanguage = null;
        outer:
        for (String arg : args) {
            for (Language language : Language.values()) {
                if (language.getCode().equalsIgnoreCase(arg) || language.getName().equalsIgnoreCase(arg)) {
                    targetLanguage = language;
                    break outer;
                }
            }
        }
        if (targetLanguage == null) {
            MessageUtil.sendMessage(sender, ChatColor.DARK_AQUA + "DiscordSRV is currently in " + currentLanguageName + ". " +
                    "Change it by giving a language as an argument.");
            return;
        }
        String targetLanguageName = StringUtils.capitalize(targetLanguage.getName().toLowerCase());

        if (!DiscordSRV.config().isLanguageAvailable(targetLanguage)) {
            String available = Arrays.stream(Language.values())
                    .filter(DiscordSRV.config()::isLanguageAvailable)
                    .map(language -> StringUtils.capitalize(language.getName().toLowerCase()))
                    .collect(Collectors.joining(", "));
            MessageUtil.sendMessage(sender, ChatColor.DARK_AQUA + "DiscordSRV does not have a translation for " + targetLanguageName + ". " +
                    "Supported languages are as follows: " + available + ".");
            return;
        }

        if (Arrays.stream(args).noneMatch(s -> s.equalsIgnoreCase("-confirm"))) {
            TextComponent message = Component.text("This will reset your DiscordSRV configuration files to be in ", NamedTextColor.DARK_AQUA)
                    .append(Component.text(targetLanguageName, NamedTextColor.WHITE))
                    .append(Component.text(". Your old config files will be renamed to have ", NamedTextColor.DARK_AQUA))
                    .append(Component.text(currentLanguageName + ".", NamedTextColor.WHITE))
                    .append(Component.text(" on the beginning of the file name. "))
                    .append(Component.text("[Confirm" + (sender instanceof Player ? "?" : " by running the command again, adding \" -confirm\" to the end") + "]")
                            .color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/discord language " + targetLanguage.getCode() + " -confirm"))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to confirm the config change.", NamedTextColor.GREEN)))
                    );
            MessageUtil.sendMessage(sender, message);
        } else {
            DiscordSRV.config().setLanguage(targetLanguage);

            for (Map.Entry<Source, Provider> entry : DiscordSRV.config().getSources().entrySet()) {
                File source = entry.getKey().getFile();
                File target = new File(source.getParentFile(), currentLanguageName + "." + source.getName());
                FileUtils.moveFile(source, target);

                entry.getValue().saveDefaults();

                // set the ForcedLanguage value to the new language so language change will be persistent
                if (entry.getKey().getResourceName().equals("config")) {
                    String file = FileUtils.readFileToString(source, "UTF-8");
                    file = file.replace("\nForcedLanguage: none", "\nForcedLanguage: " + targetLanguageName);
                    FileUtils.writeStringToFile(source, file, "UTF-8");
                }
            }

            MessageUtil.sendMessage(sender, ChatColor.DARK_AQUA + "DiscordSRV language successfully changed to " + targetLanguageName + ".");
        }
    }

}
