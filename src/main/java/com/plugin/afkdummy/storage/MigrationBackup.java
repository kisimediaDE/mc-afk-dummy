package com.plugin.afkdummy.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Back up original settings and sessions before the fork can modify them. */
public final class MigrationBackup {
    private MigrationBackup() {}
    public static void prepare(Path folder) {
        try {
            Files.createDirectories(folder);
            Path marker = folder.resolve(".free-timer-v1");
            if (Files.exists(marker)) return;
            Path backup = folder.resolve("backup-before-free-timer-" + System.currentTimeMillis());
            for (String name : new String[]{"config.yml", "config.json", "dummies.json"}) {
                Path original = folder.resolve(name);
                if (Files.exists(original)) {
                    Files.createDirectories(backup);
                    Files.copy(original, backup.resolve(name));
                }
            }
            Files.writeString(marker, "Original settings backed up before first fork startup.\n");
        } catch (IOException e) {
            throw new IllegalStateException("Migration backup failed; refusing to change existing data", e);
        }
    }
}
