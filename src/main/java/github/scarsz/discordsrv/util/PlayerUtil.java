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

package github.scarsz.discordsrv.util;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.hooks.PluginHook;
import github.scarsz.discordsrv.hooks.vanish.VanishHook;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
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
            DiscordSRV.error(e);
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
            DiscordSRV.debug(Debug.DISCORD_TO_MINECRAFT, "Tried notifying players with null or blank message");
            return;
        }

        List<String> splitMessage =
                Arrays.stream(MessageUtil.strip(message).replaceAll("[^a-zA-Z0-9_@]", " ").split(" ")) // split message by groups of alphanumeric characters & underscores
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
                        splitMessage.contains("@" + player.getName().toLowerCase()) || splitMessage.contains("@" + MessageUtil.strip(player.getDisplayName().toLowerCase()))
                )
                .forEach(player -> player.playSound(player.getLocation(), notificationSound, 1, 1));
    }

    /**
     * Check if the given Player is vanished by a supported and hooked vanish plugin
     * @param player Player to check
     * @return whether or not the player is vanished
     */
    public static boolean isVanished(Player player) {
        for (PluginHook pluginHook : DiscordSRV.getPlugin().getPluginHooks()) {
            if (pluginHook instanceof VanishHook) {
                if (((VanishHook) pluginHook).isVanished(player)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static int getPing(Player player) {
        try {
            Object entityPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return (int) entityPlayer.getClass().getField("ping").get(entityPlayer);
        } catch (Exception e) {
            DiscordSRV.error(e);
            return -1;
        }
    }

    private static final List<Character> VANILLA_TARGET_SELECTORS = Arrays.asList('p', 'r', 'a', 'e', 's');

    public static String convertTargetSelectors(String message, CommandSender sender) {
        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == '@') {
                int end = getSelectorEnd(message, i);
                if (end < 0 || end + 1 < message.length() && !canSeparateSelectors(message.charAt(end + 1))) {
                    continue;
                }
                String selector = message.substring(i, end + 1);

                try {
                    String target = sender == null ? "{TARGET}" : sender.getServer().selectEntities(sender, selector).stream()
                            .map(Entity::getName)
                            .collect(Collectors.joining(" "));
                    message = message.substring(0, i) + target + message.substring(end + 1);
                    i += target.length() - 1;
                } catch (Exception ignored) {
                    // 1.12 and below or invalid selector
                }
            }
        }
        return message;
    }

    /**
     * Seeks the position of the end character of the selector.
     *
     * @param message the full raw message
     * @param start the position of selector start
     * @return the index of the last character or -1 if invalid
     */
    private static int getSelectorEnd(String message, int start) {
        int end = start + 1;
        if (end >= message.length() || !VANILLA_TARGET_SELECTORS.contains(message.charAt(end))) {
            return -1; // Not a valid selector type
        }

        int argsPos = start + 2;
        if (argsPos < message.length() && message.charAt(argsPos) == '[') {
            for (int i = argsPos + 1; i < message.length(); i++) {
                char current = message.charAt(i);
                if (current == '[' || Character.isWhitespace(current)) {
                    return -1; // Selectors args cannot be recursive or contain spaces
                }
                if (current == ']') {
                    return i;
                }
            }
            return -1; // No end to the arguments
        }

        return end;
    }

    /**
     * Determines whether a character can separate two selectors.
     *
     * <p>Unlike the vanilla behavior, it is safer to not execute
     * target selectors like {@code @everyone} to avoid confusion.</p>
     *
     * @param character the character
     * @return if it could separate a selector from the rest
     */
    private static boolean canSeparateSelectors(char character) {
        return Character.isWhitespace(character) || character == '@';
    }

    /**
     * Returns whether the passed UUID is a v3 UUID. Offline UUIDs are v3, online are v4.
     * @param uuid the UUID to check
     * @return whether the UUID is a v3 UUID & thus is offline
     */
    public static boolean uuidIsOffline(UUID uuid) {
        return uuid.version() == 3;
    }

}
