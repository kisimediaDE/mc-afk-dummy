package com.plugin.afkdummy.probe;

import com.plugin.afkdummy.AFKDummyPlugin;
import com.plugin.afkdummy.entity.DummyPlayer;
import org.bukkit.command.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

/** Console-only test driver for an isolated server; absent from the release JAR. */
public final class Probe extends JavaPlugin {
    private final List<DummyPlayer> actors = new ArrayList<>();
    private AFKDummyPlugin target;
    public void onEnable() { target = (AFKDummyPlugin) getServer().getPluginManager().getPlugin("AFKDummy"); }
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof ConsoleCommandSender)) return true;
        try {
            if (args.length > 0 && args[0].equals("bench")) {
                new LoadBenchmark(this, target).start(args[1]);
                return true;
            }
            if (args.length > 0 && args[0].equals("logout")) {
                actors.forEach(DummyPlayer::remove); actors.clear();
                getLogger().info("PROBE OWNERS OFFLINE"); return true;
            }
            if (args.length > 0 && args[0].equals("inspect")) {
                for (var session : target.getDummyManager().getAllSessions().values()) {
                    var dummy = session.getDummyPlayer();
                    var player = dummy.getBukkitPlayer();
                    if (!player.isValid() || getServer().getPlayer(player.getUniqueId()) != player || !player.getWorld().getPlayers().contains(player)) throw new AssertionError("Inconsistent player registration");
                    if (!target.getDummyManager().isDummyPlayer(player)
                            || target.getDummyManager().getSessionByEntityId(player.getEntityId()).orElseThrow() != session)
                        throw new AssertionError("Inconsistent dummy lookup index");
                    var nearby = player.getWorld().getNearbyEntities(player.getLocation(), 128, 128, 128);
                    long monsters = nearby.stream().filter(org.bukkit.entity.Monster.class::isInstance).count();
                    getLogger().info("PROBE STATE " + session.getSessionId() + " remaining=" + session.getRemainingTimeMs() + " monsters=" + monsters + " chunks=" + player.getWorld().getLoadedChunks().length);
                }
                getLogger().info("PROBE INSPECT PASS online=" + getServer().getOnlinePlayers().size()); return true;
            }
            if (args.length > 0 && args[0].equals("clear")) {
                target.getDummyManager().despawnAll();
                target.getStorageManager().clear();
                actors.forEach(DummyPlayer::remove); actors.clear();
                getLogger().info("PROBE CLEARED"); return true;
            }
            int count = args.length > 0 ? Integer.parseInt(args[0]) : 1;
            long duration = args.length > 1 ? Long.parseLong(args[1]) : 15000;
            for (int i = 0; i < count; i++) {
                var location = getServer().getWorlds().getFirst().getSpawnLocation().add(i * 160, 3, 0);
                var actor = new DummyPlayer(UUID.randomUUID(), "Probe" + i, location, UUID.randomUUID(), this);
                actors.add(actor); actor.spawn(); actor.getBukkitPlayer().setOp(true);
                var session = target.getDummyManager().spawnDummy(actor.getBukkitPlayer(), location, duration);
                if (session == null) throw new AssertionError("Managed spawn failed");
                if (!session.getLocation().getWorld().equals(location.getWorld()) || session.getLocation().distanceSquared(location) > 1) throw new AssertionError("Wrong spawn position");
                if (target.getDummyManager().spawnDummy(actor.getBukkitPlayer(), location, duration) != null) throw new AssertionError("Owner limit ignored");
                getLogger().info("PROBE SPAWN " + session.getSessionId() + " " + session.getFormattedTimeRemaining());
            }
            getLogger().info("PROBE PASS count=" + target.getDummyManager().getActiveCount());
        } catch (Throwable e) { getLogger().log(java.util.logging.Level.SEVERE, "PROBE FAILED", e); }
        return true;
    }
    public void onDisable() { actors.forEach(actor -> { try { actor.remove(); } catch (Exception ignored) {} }); }
}
