package com.plugin.afkdummy.storage;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PausedStorageTest {
    @Test void lateAsyncSaveCannotOverwriteFinalShutdownSnapshot(@TempDir Path folder) throws Exception {
        var plugin = org.mockito.Mockito.mock(org.bukkit.plugin.Plugin.class);
        var scheduler = org.mockito.Mockito.mock(org.bukkit.scheduler.BukkitScheduler.class);
        org.mockito.Mockito.when(plugin.getDataFolder()).thenReturn(folder.toFile());
        org.mockito.Mockito.when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        org.mockito.Mockito.when(plugin.isEnabled()).thenReturn(true);
        var queued = new java.util.ArrayList<Runnable>();
        org.mockito.Mockito.doAnswer(inv -> { queued.add(inv.getArgument(1, Runnable.class)); return null; })
                .when(scheduler).runTaskAsynchronously(org.mockito.ArgumentMatchers.eq(plugin), org.mockito.ArgumentMatchers.any(Runnable.class));
        try (var bukkit = org.mockito.Mockito.mockStatic(org.bukkit.Bukkit.class)) {
            bukkit.when(org.bukkit.Bukkit::getScheduler).thenReturn(scheduler);
            var storage = new StorageManager(plugin);
            var data = new DummyData(UUID.randomUUID(), UUID.randomUUID(), "Owner", 1, "world", 0, 80, 0, 0, 0, 1L);
            data.setRemainingMillis(90000);
            storage.addEntry(data);
            data.setRemainingMillis(45000);
            storage.saveSync();
            queued.getFirst().run();
            var restored = new Gson().fromJson(Files.readString(folder.resolve("dummies.json")), DummyData[].class);
            assertEquals(45000, restored[0].getRemainingTimeMs());
        }
    }
    @Test void pausedSessionSurvivesOldExpiryAndJsonRoundTrip() {
        var data = new DummyData(UUID.randomUUID(), UUID.randomUUID(), "Owner", 1, "world", 0, 80, 0, 0, 0, 1L);
        data.setRemainingMillis(90000);
        var restored = new Gson().fromJson(new Gson().toJson(data), DummyData.class);
        assertEquals(90000, restored.getRemainingTimeMs());
        assertFalse(restored.isExpired());
        restored.setRemainingMillis(0);
        assertTrue(restored.isExpired());
    }
    @Test void backupPreservesOriginalBytesAndDoesNotRepeat(@TempDir Path folder) throws Exception {
        Files.writeString(folder.resolve("dummies.json"), "original");
        MigrationBackup.prepare(folder);
        Files.writeString(folder.resolve("dummies.json"), "updated");
        MigrationBackup.prepare(folder);
        try (var dirs = Files.list(folder)) {
            var backups = dirs.filter(Files::isDirectory).toList();
            assertEquals(1, backups.size());
            assertEquals("original", Files.readString(backups.getFirst().resolve("dummies.json")));
        }
    }
}
