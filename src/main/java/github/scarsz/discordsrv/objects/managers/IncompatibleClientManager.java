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

package github.scarsz.discordsrv.objects.managers;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.LangUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class IncompatibleClientManager implements PluginMessageListener, Listener {

    private static final Method CLIENT_BRAND_NAME_METHOD;

    static {
        Method method = null;
        try {
            method = Player.class.getMethod("getClientBrandName");
        } catch (NoSuchMethodError | NoSuchMethodException ignored) {}
        CLIENT_BRAND_NAME_METHOD = method;
    }

    private final Set<UUID> incompatibleClients = new HashSet<>();

    public boolean isIncompatible(Player player) {
        return incompatibleClients.contains(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (CLIENT_BRAND_NAME_METHOD == null) return;

        // Client brand is not available during this time, so we run check brand 2s and 10s after this point
        Bukkit.getScheduler().runTaskLaterAsynchronously(
                DiscordSRV.getPlugin(), () -> checkBrand(event.getPlayer()), 40L);
        Bukkit.getScheduler().runTaskLaterAsynchronously(
                DiscordSRV.getPlugin(), () -> checkBrand(event.getPlayer()), 200L);
    }

    private void checkBrand(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        String brand;
        try {
            brand = (String) CLIENT_BRAND_NAME_METHOD.invoke(player);
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return;
        }

        if (brand != null && brand.toLowerCase(Locale.ROOT).startsWith("lunarclient")) {
            addIncompatible(player, "LunarClient");
            DiscordSRV.debug("Detected client brand: " + brand + " for " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
        checkChannel(event.getPlayer(), event.getChannel(), true);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] bytes) {
        checkChannel(player, channel, false);
    }

    private void checkChannel(Player player, String channel, boolean register) {
        if (channel.toLowerCase(Locale.ROOT).startsWith("lunarclient")) {
            addIncompatible(player, "LunarClient");
            DiscordSRV.debug("Received " + (register ? "message channel register" : "plugin message")
                                     + " from channel " + channel + " for " + player.getName());
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void addIncompatible(Player player, String client) {
        if (!incompatibleClients.add(player.getUniqueId())) {
            return;
        }

        if (!DiscordSRV.config().getBooleanElse("EnableIncompatibleClientAlert", true)) {
            return;
        }

        // Skip Adventure for this in case the client can't even receive that
        player.sendMessage(ChatColor.RED + LangUtil.InternalMessage.INCOMPATIBLE_CLIENT.toString().replace("{client}", client));

        DiscordSRV.info(player.getName() + " was sent a notice for having a degraded user experience due to " + client
                                + " (You can use EnableIncompatibleClientAlert to disable the message if you'd like (Not recommended))");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        incompatibleClients.remove(event.getPlayer().getUniqueId());
    }
}
