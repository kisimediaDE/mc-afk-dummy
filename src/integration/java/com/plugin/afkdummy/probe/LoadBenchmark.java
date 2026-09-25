package com.plugin.afkdummy.probe;

import com.plugin.afkdummy.AFKDummyPlugin;
import com.plugin.afkdummy.entity.DummyPlayer;
import com.plugin.afkdummy.listener.PlayerListener;
import com.plugin.afkdummy.util.SkinUtil;
import com.mojang.authlib.properties.Property;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.util.*;

/** Synthetic local benchmark, never included in the plugin release. */
final class LoadBenchmark implements Listener {
    private static final int WARMUP = 200, SAMPLES = 400;
    private final JavaPlugin probe;
    private final AFKDummyPlugin target;
    private final World world;
    private final PlayerListener listener;
    private final List<Entity> mobs = new ArrayList<>();
    private final List<Map<String, Object>> results = new ArrayList<>();
    private final double[] ticks = new double[SAMPLES];
    private final com.sun.management.OperatingSystemMXBean os =
            (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private final int[] counts = {0, 1, 3, 6, 6};
    private BukkitTask task;
    private int phase = -1, elapsed, samples;
    private long started, cpuStart, gcStart, workloadNanos;
    private boolean measuring;
    private String label;
    private Block piston;
    private List<Block> pushed;

    LoadBenchmark(JavaPlugin probe, AFKDummyPlugin target) {
        this.probe = probe; this.target = target;
        world = probe.getServer().getWorlds().getFirst();
        listener = new PlayerListener(target);
    }

    void start(String label) throws Exception {
        if (task != null) throw new IllegalStateException("Benchmark already started");
        this.label = label;
        if (target.getDummyManager().getActiveCount() != 0) throw new IllegalStateException("Requires empty server");
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setTime(6000); world.setStorm(false); world.setAutoSave(false);
        // Remove network variability: preload deterministic synthetic owners' skins.
        var field = SkinUtil.class.getDeclaredField("SKIN_CACHE"); field.setAccessible(true);
        @SuppressWarnings("unchecked") var cache = (Map<UUID, Property>) field.get(null);
        for (int i = 0; i < 6; i++) cache.put(owner(i), new Property("textures", "eyJ0ZXh0dXJlcyI6e319", ""));
        for (int i = 0; i < 6; i++) {
            int x0 = i * 160;
            for (int cx = -3; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++)
                world.getChunkAt((x0 >> 4) + cx, cz);
            for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                world.getBlockAt(x0 + x, 79, z).setType(Material.STONE, false);
                for (int y = 80; y <= 83; y++)
                    world.getBlockAt(x0 + x, y, z).setType(
                            Math.abs(x) == 8 || Math.abs(z) == 8 ? Material.GLASS : Material.AIR, false);
            }
        }
        piston = world.getBlockAt(20, 80, 0);
        piston.setType(Material.PISTON, false);
        pushed = new ArrayList<>();
        for (int x = 21; x <= 32; x++) pushed.add(world.getBlockAt(x, 80, 0));
        probe.getServer().getPluginManager().registerEvents(this, probe);
        next();
        task = probe.getServer().getScheduler().runTaskTimer(probe, () -> {
            elapsed++;
            if (elapsed == WARMUP) {
                started = System.nanoTime(); cpuStart = os.getProcessCpuTime(); gcStart = gcMillis();
                workloadNanos = 0; measuring = true;
            }
            if (phase == 4) {
                long before = System.nanoTime();
                // Deliberately excessive plugin-only stress; no claim of a real farm workload.
                for (int i = 0; i < 2000; i++)
                    listener.onPistonExtend(new BlockPistonExtendEvent(piston, pushed, BlockFace.EAST));
                for (int i = 0; i < 10; i++) target.getDummyManager().checkpoint();
                if (measuring) workloadNanos += System.nanoTime() - before;
            }
        }, 1L, 1L);
    }

    private static UUID owner(int i) {
        return UUID.nameUUIDFromBytes(("benchmark-owner-" + i).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private void next() {
        measuring = false;
        target.getDummyManager().despawnAll(); target.getStorageManager().clear();
        mobs.forEach(Entity::remove); mobs.clear();
        phase++; elapsed = 0; samples = 0;
        if (phase == counts.length) {
            task.cancel(); HandlerList.unregisterAll(this);
            try {
                var output = probe.getDataFolder().toPath(); Files.createDirectories(output);
                Files.writeString(output.resolve("benchmark-" + label + ".json"),
                        new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(results));
                probe.getLogger().info("BENCH DONE " + label);
            } catch (Exception e) { throw new RuntimeException(e); }
            return;
        }
        for (int i = 0; i < counts[phase]; i++) {
            Location loc = new Location(world, i * 160, 81, 0);
            var actor = new DummyPlayer(owner(i), "Bench" + i, loc, UUID.randomUUID(), probe);
            actor.spawn(); actor.getBukkitPlayer().setOp(true);
            try {
                if (target.getDummyManager().spawnDummy(actor.getBukkitPlayer(), loc, 600000) == null)
                    throw new IllegalStateException("Benchmark spawn failed");
            } finally { actor.remove(); }
            for (int m = 0; m < 32; m++) {
                var mob = world.spawn(new Location(world, i * 160 - 6 + (m % 8) * 1.5, 80, -5 + (m / 8) * 3), Husk.class);
                mob.setRemoveWhenFarAway(false); mob.setPersistent(true); mob.setSilent(true);
                mobs.add(mob);
            }
        }
        probe.getLogger().info("BENCH PHASE " + label + " " + phase + " dummies=" + counts[phase]);
    }

    @EventHandler public void onTick(ServerTickEndEvent event) {
        if (!measuring) return;
        ticks[samples++] = event.getTickDuration();
        if (samples < SAMPLES) return;
        measuring = false;
        long wall = System.nanoTime() - started;
        double[] sorted = ticks.clone(); Arrays.sort(sorted);
        var row = new LinkedHashMap<String, Object>();
        row.put("label", label); row.put("phase", phase); row.put("dummies", counts[phase]);
        row.put("workload", phase == 4 ? "2000 piston checks + 10 checkpoints per tick" : "32 AI husks per dummy");
        row.put("ticks", samples); row.put("meanMspt", Arrays.stream(sorted).average().orElseThrow());
        row.put("p50Mspt", sorted[199]); row.put("p95Mspt", sorted[379]); row.put("p99Mspt", sorted[395]);
        row.put("maxMspt", sorted[399]); row.put("ticksOver50ms", Arrays.stream(sorted).filter(v -> v > 50).count());
        row.put("wallSeconds", wall / 1e9); row.put("cpuCoreEquivalent", (os.getProcessCpuTime() - cpuStart) / (double) wall);
        row.put("gcMillis", gcMillis() - gcStart); row.put("heapUsedMiB", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576.0);
        row.put("stressMainMsPerTick", workloadNanos / 1e6 / SAMPLES);
        row.put("loadedChunks", world.getLoadedChunks().length); row.put("entities", world.getEntities().size());
        row.put("validMobs", mobs.stream().filter(Entity::isValid).count());
        results.add(row);
        probe.getLogger().info("BENCH RESULT " + new com.google.gson.Gson().toJson(row));
        probe.getServer().getScheduler().runTask(probe, this::next);
    }

    private static long gcMillis() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream().mapToLong(b -> Math.max(0, b.getCollectionTime())).sum();
    }
}
