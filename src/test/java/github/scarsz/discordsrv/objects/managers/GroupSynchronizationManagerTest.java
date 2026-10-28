/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.objects.managers;

import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager.SyncCause;
import github.scarsz.discordsrv.objects.managers.GroupSynchronizationManager.SyncDirection;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GroupSynchronizationManagerTest {
    private static final UUID PLAYER_ID = UUID.fromString("53a92ced-c225-4e98-abeb-fb87da3fc79b");

    @Test
    void overlappingTimerPassIsSkippedAndLaterPassCanRun() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        GroupSynchronizationManager manager = new GroupSynchronizationManager() {
            @Override
            void resyncPlayers(SyncDirection direction, SyncCause cause) {
                assertEquals(SyncDirection.AUTHORITATIVE, direction);
                assertEquals(SyncCause.TIMER, cause);
                if (calls.incrementAndGet() == 1) {
                    entered.countDown();
                    try {
                        assertTrue(release.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }
            }
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> manager.resync(SyncDirection.AUTHORITATIVE, SyncCause.TIMER));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            Future<?> overlapping = executor.submit(() -> manager.resync(SyncDirection.AUTHORITATIVE, SyncCause.TIMER));
            overlapping.get(5, TimeUnit.SECONDS);
            assertEquals(1, calls.get(), "An overlapping timer must not run or queue another sweep");
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            manager.resync(SyncDirection.AUTHORITATIVE, SyncCause.TIMER);
            assertEquals(2, calls.get());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void failedTimerPassDoesNotBlockFuturePasses() {
        AtomicInteger calls = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException("lookup failed");
        GroupSynchronizationManager manager = new GroupSynchronizationManager() {
            @Override
            void resyncPlayers(SyncDirection direction, SyncCause cause) {
                if (calls.incrementAndGet() == 1) throw failure;
            }
        };
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> manager.resync(SyncDirection.AUTHORITATIVE, SyncCause.TIMER)));
        manager.resync(SyncDirection.AUTHORITATIVE, SyncCause.TIMER);
        assertEquals(2, calls.get());
    }

    @Test
    void explicitPassAndPlayerEventAreNotSuppressedByTimer() {
        AtomicInteger explicitCalls = new AtomicInteger();
        AtomicInteger playerCalls = new AtomicInteger();
        OfflinePlayer player = offlinePlayer(null);
        GroupSynchronizationManager manager = new GroupSynchronizationManager() {
            @Override
            void resyncPlayers(SyncDirection direction, SyncCause cause) {
                if (cause == SyncCause.TIMER) {
                    resync(SyncDirection.TO_DISCORD, SyncCause.MANUAL);
                    resync(player, SyncDirection.TO_DISCORD, false, SyncCause.PLAYER_LINK);
                } else {
                    assertEquals(SyncDirection.TO_DISCORD, direction);
                    explicitCalls.incrementAndGet();
                }
            }

            @Override
            public void resync(OfflinePlayer target, SyncDirection direction, boolean addLinkedRole, SyncCause cause) {
                assertSame(player, target);
                assertEquals(SyncCause.PLAYER_LINK, cause);
                playerCalls.incrementAndGet();
            }
        };
        manager.resync(SyncDirection.AUTHORITATIVE, SyncCause.TIMER);
        assertEquals(1, explicitCalls.get());
        assertEquals(1, playerCalls.get());
    }

    @Test
    void offlineDiagnosticsUseUuidWithoutLoadingName() {
        assertEquals("53a92ced-c225-4e98-abeb-fb87da3fc79b",
                GroupSynchronizationManager.getPlayerIdentifier(offlinePlayer(null)));
    }

    @Test
    void onlineDiagnosticsUseLivePlayerName() {
        Player online = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getName")) return "OnlinePlayer";
                    throw new AssertionError("Unexpected live-player lookup: " + method.getName());
                });
        assertEquals("OnlinePlayer (53a92ced-c225-4e98-abeb-fb87da3fc79b)",
                GroupSynchronizationManager.getPlayerIdentifier(offlinePlayer(online)));
    }

    private static OfflinePlayer offlinePlayer(Player online) {
        return (OfflinePlayer) Proxy.newProxyInstance(OfflinePlayer.class.getClassLoader(),
                new Class<?>[]{OfflinePlayer.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getUniqueId")) return PLAYER_ID;
                    if (method.getName().equals("getPlayer")) return online;
                    throw new AssertionError("Unexpected offline-player lookup: " + method.getName());
                });
    }
}
