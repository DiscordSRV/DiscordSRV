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

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.hooks.vanish.EssentialsHook;
import github.scarsz.discordsrv.hooks.vanish.PhantomAdminHook;
import github.scarsz.discordsrv.hooks.vanish.SuperVanishHook;
import github.scarsz.discordsrv.hooks.vanish.VanishNoPacketHook;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PlayerUtil {

    public static List<Player> getOnlinePlayers() {
        return getOnlinePlayers(false);
    }

    /**
     * Method return type-safe version of Bukkit::getOnlinePlayers
     * @param filterVanishedPlayers whether or not to filter out vanished players
     * @return {@code ArrayList} containing online players
     */
    public static List<Player> getOnlinePlayers(boolean filterVanishedPlayers) {
        List<Player> onlinePlayers = new ArrayList<>();

        try {
            Method onlinePlayerMethod = Server.class.getMethod("getOnlinePlayers");
            if (onlinePlayerMethod.getReturnType().equals(Collection.class)) {
                for (Object o : ((Collection<?>) onlinePlayerMethod.invoke(Bukkit.getServer()))) {
                    onlinePlayers.add((Player) o);
                }
            } else {
                Collections.addAll(onlinePlayers, ((Player[]) onlinePlayerMethod.invoke(Bukkit.getServer())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!filterVanishedPlayers) {
            return onlinePlayers;
        } else {
            return onlinePlayers.stream()
                    .filter(player -> !isVanished(player))
                    .collect(Collectors.toList());
        }
    }

    private static Sound notificationSound = null;
    static {
        for (Sound sound : Sound.class.getEnumConstants())
            if (sound.name().contains("PLING")) notificationSound = sound;

        // this'll never occur, but, in the case that it really didn't find a notification sound, go with a UI button click
        if (notificationSound == null) notificationSound = Sound.UI_BUTTON_CLICK;
    }

    /**
     * Notify online players of mentions after a message was broadcasted to them
     * Uses Java 8's Steam API {@link java.util.stream.Stream#filter(Predicate)} with the given predicate to filter out online players that didn't get the message this ding is for
     * @param predicate predicate to determine whether or not the player got the message this ding was triggered for
     * @param message the message to be searched for players to ding
     */
    public static void notifyPlayersOfMentions(Predicate<? super Player> predicate, String message) {
        if (predicate == null) predicate = Objects::nonNull; // if null predicate given, that means everyone on the server would've gotten the message
                                                             // thus, default to a (hopefully) always true predicate

        if (StringUtils.isBlank(message)) {
            DiscordSRV.debug("Tried notifying players with null or blank message");
            return;
        }

        List<String> splitMessage =
                Arrays.stream(DiscordUtil.strip(message).replaceAll("[^a-zA-Z0-9_@]", " ").split(" ")) // split message by groups of alphanumeric characters & underscores
                        .filter(StringUtils::isNotBlank) // not actually needed but it cleans up the stream a lot
                        .map(String::toLowerCase) // map everything to be lower case because we don't care about case when finding player names
                        .map(s -> {
                            String possibleId = s.replace("<@", "").replace(">", "");
                            if (StringUtils.isNotBlank(possibleId) && StringUtils.isNumeric(possibleId) && s.startsWith("<@") && s.endsWith(">")) {
                                User possibleUser = DiscordUtil.getUserById(possibleId);
                                if (possibleUser == null) return s;
                                return "@" + DiscordSRV.getPlugin().getMainGuild().getMember(possibleUser).getEffectiveName();
                            } else {
                                return s;
                            }
                        })
                        .collect(Collectors.toList());

        getOnlinePlayers().stream()
                .filter(predicate) // apply predicate to filter out players that didn't get this message sent to them
                .filter(player -> // filter out players who's name nor display name is in the split message
                        splitMessage.contains("@" + player.getName().toLowerCase()) || splitMessage.contains("@" + DiscordUtil.strip(player.getDisplayName().toLowerCase()))
                )
                .forEach(player -> player.playSound(player.getLocation(), notificationSound, 1, 1));
    }

    /**
     * Check if the given Player is vanished by a supported and hooked vanish plugin
     * @param player Player to check
     * @return whether or not the player is vanished
     */
    public static boolean isVanished(Player player) {
        if (PluginUtil.pluginHookIsEnabled("vanishnopacket")) {
            return VanishNoPacketHook.isVanished(player);
        } else if (PluginUtil.pluginHookIsEnabled("supervanish") || PluginUtil.pluginHookIsEnabled("premiumvanish")) {
            return SuperVanishHook.isVanished(player);
        } else if (PluginUtil.pluginHookIsEnabled("phantomadmin")){
            return PhantomAdminHook.isVanished(player);
        } else if (PluginUtil.pluginHookIsEnabled("essentials")) {
            return EssentialsHook.isVanished(player);
        }
        return false;
    }

}
