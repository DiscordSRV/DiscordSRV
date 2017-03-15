package github.scarsz.discordsrv.util;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 4:09 PM
 */
public class PlayerUtil {

    /**
     * Method return type-safe version of Bukkit::getOnlinePlayers
     * @return {@code ArrayList} containing online players
     */
    public static List<Player> getOnlinePlayers() {
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

        return onlinePlayers;
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

        List<String> splitMessage =
                Arrays.stream(message.replaceAll("[^a-zA-Z0-9_]", " ").split(" ")) // split message by groups of alphanumeric characters & underscores
                .filter(StringUtils::isNotBlank) // not actually needed but it cleans up the stream a lot
                .map(String::toLowerCase) // map everything to be lower case because we don't care about case when finding player names
                .collect(Collectors.toList());

        getOnlinePlayers().stream()
                .filter(predicate) // apply predicate to filter out players that didn't get this message sent to them
                .filter(player -> // filter out players who's names and display names aren't in the split message
                        splitMessage.contains(player.getName().toLowerCase()) ||
                        splitMessage.contains(ChatColor.stripColor(player.getDisplayName().toLowerCase()))
                )
                .forEach(player -> player.playSound(player.getLocation(), notificationSound, 1, 1));
    }

}
