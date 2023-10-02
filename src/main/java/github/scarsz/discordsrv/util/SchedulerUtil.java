/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2022 Austin "Scarsz" Shapiro
 *
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
 */

package github.scarsz.discordsrv.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SchedulerUtil {

    private static Boolean IS_FOLIA = null;
    private static Object GLOBAL_REGION_SCHEDULER = null;
    private static Object ASYNC_SCHEDULER = null;

    public static <T> T callMethod(Class<?> clazz, Object object, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            return (T) clazz.getDeclaredMethod(methodName, parameterTypes).invoke(object, args);
        } catch (Throwable t) {
            throw new IllegalStateException(t);
        }
    }

    public static <T> T callMethod(Object object, String methodName, Class<?>[] parameterTypes, Object... args) {
        return callMethod(object.getClass(), object, methodName, parameterTypes, args);
    }

    public static <T> T callMethod(Object object, String methodName) {
        return callMethod(object.getClass(), null, methodName, new Class[]{});
    }

    public static <T> T callMethod(Class<?> clazz, String methodName) {
        return callMethod(clazz, null, methodName, new Class[]{});
    }

    private static boolean methodExist(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            clazz.getDeclaredMethod(methodName, parameterTypes);
            return true;
        } catch (Throwable ignored) {}
        return false;
    }

    public static Boolean isFolia() {
        if (IS_FOLIA == null) IS_FOLIA = methodExist(Bukkit.class, "getGlobalRegionScheduler");
        return IS_FOLIA;
    }

    public static Object getGlobalRegionScheduler() {
        if (GLOBAL_REGION_SCHEDULER == null) {
            GLOBAL_REGION_SCHEDULER = callMethod(Bukkit.class, "getGlobalRegionScheduler");
        }
        return GLOBAL_REGION_SCHEDULER;
    }

    public static Object getAsyncScheduler() {
        if (ASYNC_SCHEDULER == null) {
            ASYNC_SCHEDULER = callMethod(Bukkit.class, "getAsyncScheduler");
        }
        return ASYNC_SCHEDULER;
    }

    public static void runTask(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            Object globalRegionScheduler = getGlobalRegionScheduler();
            callMethod(globalRegionScheduler, "run", new Class[]{Plugin.class, Consumer.class}, plugin, (Consumer<?>) (task) -> runnable.run());
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runTaskAsynchronously(Plugin plugin, Runnable runnable) {
        if (isFolia()) {
            Object asyncScheduler = getAsyncScheduler();
            callMethod(asyncScheduler, "runNow", new Class[]{Plugin.class, Consumer.class}, plugin, (Consumer<?>) (task) -> runnable.run());
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public static void runTaskTimerAsynchronously(Plugin plugin, Runnable runnable, long initialDelayTicks, long periodTicks) {
        if (isFolia()) {
            Object asyncScheduler = getAsyncScheduler();
            callMethod(asyncScheduler, "runAtFixedRate", new Class[]{Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class},
                       plugin, (Consumer<?>) (task) -> runnable.run(), initialDelayTicks, periodTicks*50, TimeUnit.MILLISECONDS);
            return;
        }
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, initialDelayTicks, periodTicks);
    }


    public static void runTaskLater(Plugin plugin, Runnable runnable, long delayedTicks) {
        if (isFolia()) {
            Object globalRegionScheduler = getGlobalRegionScheduler();
            callMethod(globalRegionScheduler, "runDelayed", new Class[]{Plugin.class, Consumer.class, long.class},
                       plugin, (Consumer<?>) (task) -> runnable.run(), delayedTicks);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, runnable, delayedTicks);
    }

    public static void runTaskLaterAsynchronously(Plugin plugin, Runnable runnable, long delayedTicks) {
        if (isFolia()) {
            Object asyncScheduler = callMethod(Bukkit.class, "getAsyncScheduler");
            callMethod(asyncScheduler, "runDelayed", new Class[]{Plugin.class, Consumer.class, long.class, TimeUnit.class},
                       plugin, (Consumer<?>) (task) -> runnable.run(), delayedTicks*50, TimeUnit.MILLISECONDS);
            return;
        }
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, delayedTicks);
    }

    public static void runTaskForPlayer(Plugin plugin, Player player, Runnable runnable) {
        if (isFolia()) {
            Object entityScheduler = callMethod(player, "getScheduler");
            callMethod(entityScheduler, "run", new Class[]{Plugin.class, Consumer.class, Runnable.class},
                plugin, (Consumer<?>) (task) -> runnable.run(), null);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void cancelTasks(Plugin plugin) {
        if (isFolia()) {
            Object asyncScheduler = getAsyncScheduler();
            Object globalRegionScheduler = getGlobalRegionScheduler();
            callMethod(asyncScheduler, "cancelTasks", new Class[]{Plugin.class}, plugin);
            callMethod(globalRegionScheduler, "cancelTasks", new Class[]{Plugin.class}, plugin);
            return;
        }
        Bukkit.getScheduler().cancelTasks(plugin);
    }
}
