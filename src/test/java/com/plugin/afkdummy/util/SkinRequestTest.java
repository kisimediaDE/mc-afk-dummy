package com.plugin.afkdummy.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SkinRequestTest {
    @Test void deduplicatesRequestsAndDispatchesCacheHitsOnMainThread() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        List<Runnable> main = new ArrayList<>(), background = new ArrayList<>();
        doAnswer(i -> { main.add(i.getArgument(1)); return null; }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
        doAnswer(i -> { background.add(i.getArgument(1)); return null; }).when(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false);
            var cache = new ConcurrentHashMap<String, String>();
            var pending = new ConcurrentHashMap<String, CompletableFuture<String>>();
            AtomicInteger fetches = new AtomicInteger();
            List<String> received = new ArrayList<>();
            for (int i = 0; i < 6; i++) SkinUtil.request("owner", cache, pending,
                    () -> { fetches.incrementAndGet(); return "skin"; }, received::add, plugin);
            assertEquals(1, background.size());
            background.removeFirst().run();
            assertEquals(1, fetches.get());
            assertTrue(received.isEmpty());
            main.forEach(Runnable::run);
            main.clear();
            assertEquals(6, received.size());
            SkinUtil.request("owner", cache, pending, () -> { fail("Cache hit must not fetch"); return null; }, received::add, plugin);
            assertEquals(6, received.size());
            assertEquals(1, main.size());
            main.removeFirst().run();
            assertEquals(7, received.size());
            assertTrue(pending.isEmpty());
            when(plugin.isEnabled()).thenReturn(false);
            SkinUtil.request("owner", cache, pending, () -> "ignored", received::add, plugin);
            assertTrue(main.isEmpty());
            assertEquals(7, received.size());
        }
    }
}
