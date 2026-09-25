package com.plugin.afkdummy.storage;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class StorageBatchingTest {
    @Test void batchesMutationsAndKeepsOnlyLatestWaitingSnapshot(@TempDir Path dir) throws Exception {
        Plugin plugin = mock(Plugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getDataFolder()).thenReturn(dir.toFile());
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        List<Runnable> main = new ArrayList<>(), background = new ArrayList<>();
        doAnswer(i -> { main.add(i.getArgument(1)); return null; }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
        doAnswer(i -> { background.add(i.getArgument(1)); return null; }).when(scheduler).runTaskAsynchronously(eq(plugin), any(Runnable.class));
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            StorageManager storage = new StorageManager(plugin);
            var data = new DummyData(UUID.randomUUID(), UUID.randomUUID(), "Owner", 1, "world", 1, 2, 3, 0, 0, 1L);
            data.setRemainingMillis(90000);
            storage.addEntry(data);
            for (int i = 0; i < 100; i++) storage.saveAsync();
            assertEquals(1, main.size(), "One snapshot task for the whole burst");
            assertFalse(Files.exists(dir.resolve("dummies.json")));
            main.removeFirst().run();
            data.setRemainingMillis(45000);
            storage.saveAsync();
            main.removeFirst().run();
            assertEquals(1, background.size(), "Slow disk must not grow the worker queue");
            data.setRemainingMillis(1000); // Must not mutate the detached 45000 snapshot.
            background.removeFirst().run();
            var saved = new Gson().fromJson(Files.readString(dir.resolve("dummies.json")), DummyData[].class);
            assertEquals(45000, saved[0].getRemainingTimeMs());
            storage.clear();
            main.removeFirst().run();
            background.removeFirst().run();
            assertEquals(0, new Gson().fromJson(Files.readString(dir.resolve("dummies.json")), DummyData[].class).length);
        }
    }
}
