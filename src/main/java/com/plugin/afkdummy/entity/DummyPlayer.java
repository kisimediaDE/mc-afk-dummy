package com.plugin.afkdummy.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.plugin.afkdummy.util.SkinUtil;
import com.plugin.afkdummy.util.DebugLogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Wraps a NMS {@link ServerPlayer} to create a fake player entity that behaves
 * identically to a real player for chunk loading and mob spawning mechanics.
 * <p>
 * The dummy player is injected into the server's player list via
 * {@link net.minecraft.server.players.PlayerList#placeNewPlayer}.
 * Supports multiple dummies per owner with unique session UUIDs.
 * </p>
 */
public class DummyPlayer {

    private final UUID sessionId;
    private final ServerPlayer handle;
    private GameProfile currentProfile;
    private final UUID ownerUUID;
    private final String ownerName;
    private Location spawnLocation;
    private final Plugin plugin;
    private String customName;
    private String skinName;

    /** The spoofed network connection, retained for lifecycle cleanup. */
    private final Connection connection;

    /** The authentication cookie used during placeNewPlayer. */
    private final CommonListenerCookie cookie;

    private boolean spawned = false;
    private boolean registrationAttempted;
    private boolean closed;
    private long skinRevision;

    /**
     * Creates a new DummyPlayer with full customization support.
     */
    public DummyPlayer(UUID ownerUUID, String ownerName, Location location, UUID sessionId,
                       String customName, String skinName, Plugin plugin) {
        this.sessionId = sessionId != null ? sessionId : UUID.randomUUID();
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.spawnLocation = location.clone();
        this.plugin = plugin;
        this.customName = customName;
        this.skinName = skinName;

        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
        // Create GameProfile with unique UUID and exact username (no extra trailing characters!)
        UUID dummyUUID = generateDummyUUID(ownerUUID, this.sessionId);
        String dummyProfileName = generateProfileName(ownerName, this.sessionId);
        GameProfile profile = new GameProfile(dummyUUID, dummyProfileName);

        // Create ClientInformation with default settings
        ClientInformation clientInfo = ClientInformation.createDefault();

        // Create the ServerPlayer entity
        this.currentProfile = profile;
        this.handle = new ServerPlayer(server, level, profile, clientInfo) {
            @Override public GameProfile getGameProfile() {
                return currentProfile != null ? currentProfile : super.getGameProfile();
            }
        };

        // Pre-set position and rotation (including head and body yaw)
        handle.setPos(location.getX(), location.getY(), location.getZ());
        handle.setRot(location.getYaw(), location.getPitch());
        handle.setYHeadRot(location.getYaw());
        handle.setYBodyRot(location.getYaw());
        handle.setOldPosAndRot();

        // Set up spoofed network connection
        this.connection = createSpoofedConnection();
        this.cookie = new CommonListenerCookie(profile, 0, clientInfo, false, "vanilla",
                java.util.concurrent.ConcurrentHashMap.newKeySet(), new io.papermc.paper.util.KeepAlive());
        setupMockPacketListener(server);
        handle.getBukkitEntity().setMetadata("NPC", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

        // Load skin asynchronously (custom skin if specified, else owner's skin)
        if (skinName != null && !skinName.trim().isEmpty()) {
            loadCustomSkin(skinName.trim(), profile);
        } else {
            loadOwnerSkin(profile);
        }
    }

    public DummyPlayer(UUID ownerUUID, String ownerName, Location location, UUID sessionId, Plugin plugin) {
        this(ownerUUID, ownerName, location, sessionId, null, null, plugin);
    }

    public DummyPlayer(UUID ownerUUID, String ownerName, Location location, Plugin plugin) {
        this(ownerUUID, ownerName, location, UUID.randomUUID(), null, null, plugin);
    }

    /**
     * Creates a spoofed {@link Connection} backed by an {@link EmbeddedChannel} with outbound release handler.
     */
    private Connection createSpoofedConnection() {
        EmbeddedChannel channel = new EmbeddedChannel(
                new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        ReferenceCountUtil.release(msg);
                    }
                },
                new ChannelOutboundHandlerAdapter() {
                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                        ReferenceCountUtil.release(msg);
                        promise.setSuccess();
                    }
                }
        ) {
            @Override
            public java.net.SocketAddress remoteAddress() {
                return new java.net.InetSocketAddress("127.0.0.1", 0);
            }

            @Override
            public java.net.SocketAddress localAddress() {
                return new java.net.InetSocketAddress("127.0.0.1", 0);
            }
        };

        Connection conn = new Connection(PacketFlow.SERVERBOUND) {
            @Override
            public void send(Packet<?> packet, io.netty.channel.ChannelFutureListener listener, boolean flush) {
                // A dummy has no network peer. Do not queue chunk packets or Netty tasks.
                if (listener != null) {
                    try { listener.operationComplete(channel.newSucceededFuture()); }
                    catch (Exception e) { throw new IllegalStateException("Dummy send callback failed", e); }
                }
            }
        };
        conn.channel = channel;
        conn.address = channel.remoteAddress();
        channel.pipeline().addLast("packet_handler", conn);

        return conn;
    }

    /**
     * Sets up the mock packet listener.
     */
    private void setupMockPacketListener(MinecraftServer server) {
        try {
            ServerGamePacketListenerImpl listener = new ServerGamePacketListenerImpl(
                    server, connection, handle, cookie) {
                @Override
                public void send(Packet<?> packet) {
                    // Outbound packets handled and released by channel pipeline
                }

                @Override
                public void disconnect(net.minecraft.network.chat.Component reason) {
                    // No-op: dummy cannot be disconnected via network
                }

                @Override
                public boolean isAcceptingMessages() {
                    return true;
                }
            };

            handle.connection = listener;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to set up mock packet listener for dummy player!", e);
            throw new IllegalStateException("Cannot create dummy player: network setup failed", e);
        }
    }

    /**
     * Loads a custom skin by player username.
     */
    private void loadCustomSkin(String username, GameProfile profile) {
        long requestRevision = ++skinRevision;
        SkinUtil.fetchSkinByNameAsync(username, (Property textures) -> {
            if (closed || requestRevision != skinRevision) return;
            if (textures != null) {
                currentProfile = SkinUtil.applySkin(currentProfile, textures);
                if (spawned) {
                    resendPlayerInfoToAll();
                }
            } else {
                // Fallback to owner skin
                loadOwnerSkin(profile);
            }
        }, plugin);
    }

    /**
     * Loads the owner's skin asynchronously and applies it to the GameProfile.
     */
    private void loadOwnerSkin(GameProfile profile) {
        long requestRevision = ++skinRevision;
        SkinUtil.fetchSkinAsync(ownerUUID, (Property textures) -> {
            if (closed || requestRevision != skinRevision) return;
            if (textures != null) {
                currentProfile = SkinUtil.applySkin(currentProfile, textures);

                // If already spawned, re-send entity and player info packets to update skin in viewports
                if (spawned) {
                    resendPlayerInfoToAll();
                }
            }
        }, plugin);
    }

    /**
     * Changes the dummy's visual display name dynamically.
     */
    public void setCustomDisplayName(String newName) {
        String oldName = this.customName;
        this.customName = (newName != null && !newName.trim().isEmpty()) ? sanitizeRawName(newName) : null;

        if (handle != null && handle.getBukkitEntity() != null) {
            String formatted = getFormattedDisplayName();

            // 1. Update Adventure Player List Name (Tab List) & Bukkit Custom Name
            handle.getBukkitEntity().playerListName(net.kyori.adventure.text.Component.text(formatted));
            handle.getBukkitEntity().customName(net.kyori.adventure.text.Component.text(formatted));
            handle.getBukkitEntity().setCustomNameVisible(true);

            // 2. Update GameProfile Name on NMS ServerPlayer so 3D nametag renders correctly (clean name, zero trailing chars!)
            // The network identity is immutable; change only display labels.

            // 3. Update Scoreboard Team for in-world styling
            updateScoreboardTeam();

            // 4. Send client refresh packets
            resendPlayerInfoToAll();

            DebugLogger.trace("DummyPlayer.java:setCustomDisplayName",
                    String.format("Updated dummy name for %s: old=\"%s\" -> new=\"%s\" (profile=\"%s\", formatted=\"%s\")",
                            ownerName, oldName, this.customName, handle.getGameProfile().name(), formatted));
        }
    }



    /**
     * Changes the dummy's skin dynamically to any player's skin by username.
     */
    public void setSkinByName(String newSkinUsername) {
        this.skinName = (newSkinUsername != null && !newSkinUsername.trim().isEmpty()) ? newSkinUsername.trim() : null;
        if (this.skinName != null) {
            loadCustomSkin(this.skinName, handle.getGameProfile());
        } else {
            loadOwnerSkin(handle.getGameProfile());
        }
    }

    public String getCustomName() {
        return customName;
    }

    public String getSkinName() {
        return skinName;
    }

    /**
     * Spawns the dummy player into the world using placeNewPlayer.
     * Writes pre-spawn playerdata to guarantee correct initial placement in the target chunk.
     */
    public void spawn() {
        if (closed) throw new IllegalStateException("Dummy already removed");
        if (spawned) {
            plugin.getLogger().warning("Attempted to spawn an already-spawned dummy for " + ownerName);
            return;
        }

        try {
            MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
            ServerLevel level = ((CraftWorld) spawnLocation.getWorld()).getHandle();

            Player existing = Bukkit.getPlayerExact(handle.getGameProfile().name());
            if (existing != null) throw new IllegalStateException("Dummy profile name is already in use");

            DebugLogger.lifecycle(sessionId.toString(), "SPAWN_START",
                    String.format("Spawning dummy for %s at %s", ownerName, formatLocation()));

            // The ServerPlayer is already constructed in the target level and position.
            // Do not write synthetic playerdata or intercept real players' login events.

            // Pre-set NMS position
            handle.setPos(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ());
            handle.setRot(spawnLocation.getYaw(), spawnLocation.getPitch());
            handle.setOldPosAndRot();

            // Inject the player into the server list and world
            registrationAttempted = true;
            server.getPlayerList().placeNewPlayer(connection, handle, cookie);
            // placeNewPlayer installs a normal listener. Replace it after registration as well.
            setupMockPacketListener(server);

            // Reset connection teleport state to prevent pending unconfirmed teleport
            if (handle.connection != null) {
                handle.connection.resetPosition();
            }

            // Post-spawn configuration
            handle.setGameMode(GameType.SURVIVAL);
            handle.getBukkitEntity().setInvulnerable(true);
            handle.setNoGravity(true);
            handle.setSilent(true);
            handle.getBukkitEntity().setCollidable(false);
            handle.getBukkitEntity().setAffectsSpawning(true);

            // Enable all 7 player skin model parts (Hat, Jacket, Left/Right Sleeves, Left/Right Pants, Cape)
            handle.getEntityData().set(net.minecraft.world.entity.player.Player.DATA_PLAYER_MODE_CUSTOMISATION, (byte) 127);

            // Exclude dummy from sleep requirement so real players can sleep
            handle.getBukkitEntity().setSleepingIgnored(true);

            // Set clean tab list display name & nametag using Adventure API
            String displayName = getFormattedDisplayName();
            handle.getBukkitEntity().playerListName(net.kyori.adventure.text.Component.text(displayName));
            handle.getBukkitEntity().customName(net.kyori.adventure.text.Component.text(displayName));
            handle.getBukkitEntity().setCustomNameVisible(true);

            // Register Scoreboard Team for in-world nametag display ([AFK] prefix)
            updateScoreboardTeam();

            // Update ChunkMap tracking to target location
            try {
                level.getChunkSource().chunkMap.move(handle);
            } catch (Throwable ignored) {}

            spawned = true;
            resendPlayerInfoToAll();

            plugin.getLogger().info("Spawned AFK dummy for " + ownerName
                    + " at " + formatLocation());
            DebugLogger.lifecycle(sessionId.toString(), "SPAWN_SUCCESS",
                    String.format("Successfully spawned dummy for %s at %s. EntityID: %d, UUID: %s",
                            ownerName, formatLocation(), handle.getId(), handle.getUUID()));

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to spawn dummy for " + ownerName, e);
            DebugLogger.lifecycle(sessionId.toString(), "SPAWN_FAIL", "Reason: " + e.getMessage());
            remove();
            throw new IllegalStateException("Dummy spawn failed", e);
        }
    }



    /**
     * Deletes any stale playerdata file for the given UUID to prevent
     * placeNewPlayer from loading old coordinates or state.
     */
    private void deletePlayerData(UUID uuid) {
        try {
            MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
            java.io.File worldFolder = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).toFile();
            java.io.File playerFile = new java.io.File(worldFolder, uuid + ".dat");
            if (playerFile.exists()) {
                if (playerFile.delete()) {
                    DebugLogger.storage("DELETE", playerFile.getAbsolutePath(), "Deleted temporary playerdata for " + uuid);
                }
            }
            java.io.File playerFileOld = new java.io.File(worldFolder, uuid + ".dat_old");
            if (playerFileOld.exists()) {
                playerFileOld.delete();
            }
        } catch (Exception e) {
            DebugLogger.log("Warning: Failed to clean playerdata for UUID " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Gets a collision-proof Scoreboard Team name dedicated to this dummy session.
     */
    private String getTeamName() {
        return "afk_" + sessionId.toString().replace("-", "").substring(0, 12);
    }

    /**
     * Updates or registers the Scoreboard Team to format the in-world nametag cleanly with [AFK] prefix.
     */
    private void updateScoreboardTeam() {
        var scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        var team = scoreboard.getTeam(getTeamName());
        if (team == null) team = scoreboard.registerNewTeam(getTeamName());
        team.addEntry(handle.getScoreboardName());
        team.prefix(net.kyori.adventure.text.Component.text(getFormattedDisplayName() + " "));
        // Bukkit sends each scoreboard update once. Never also broadcast raw team creation packets.
    }

    /**
     * Removes the dummy's Scoreboard Team upon removal.
     */
    private void removeScoreboardTeam() {
        try {
            org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            String teamName = getTeamName();
            org.bukkit.scoreboard.Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        } catch (Throwable e) {
            DebugLogger.log("Warning: Failed to remove scoreboard team for dummy: " + e.getMessage());
        }
    }

    /**
     * Teleports the dummy to a new location authoritatively.
     * Updates internal spawnLocation, calls NMS moveTo, resets connection state, and moves ChunkMap tracking.
     *
     * @param newLocation the destination Location
     */
    public void teleport(Location newLocation) {
        if (!spawned || handle == null) {
            throw new IllegalStateException("Cannot teleport an unspawned dummy");
        }
        if (newLocation == null || newLocation.getWorld() == null) {
            throw new IllegalArgumentException("Target location and world cannot be null");
        }

        this.spawnLocation = newLocation.clone();

        ServerLevel targetLevel = ((CraftWorld) newLocation.getWorld()).getHandle();
        ServerLevel currentLevel = (ServerLevel) handle.level();

        DebugLogger.trace("DummyPlayer.java:teleport",
                String.format("Teleporting dummy %s from %s to %s",
                        ownerName, formatLocation(), formatLoc(newLocation)));

        if (!targetLevel.equals(currentLevel)) {
            // Cross-world teleport
            handle.teleportTo(targetLevel, newLocation.getX(), newLocation.getY(), newLocation.getZ(),
                    java.util.Set.of(), newLocation.getYaw(), newLocation.getPitch(), true);
            handle.setYHeadRot(newLocation.getYaw());
            handle.setYBodyRot(newLocation.getYaw());
        } else {
            // Same world authoritative moveTo
            handle.setPos(newLocation.getX(), newLocation.getY(), newLocation.getZ());
            handle.setRot(newLocation.getYaw(), newLocation.getPitch());
            handle.setYHeadRot(newLocation.getYaw());
            handle.setYBodyRot(newLocation.getYaw());
            handle.setOldPosAndRot();
        }

        // Reset connection awaitingTeleport to prevent unacknowledged packet snap-backs
        if (handle.connection != null) {
            handle.connection.resetPosition();
        }

        // Update ChunkMap tracking to the new chunk
        try {
            targetLevel.getChunkSource().chunkMap.move(handle);
        } catch (Throwable ignored) {}

        DebugLogger.trace("DummyPlayer.java:teleport",
                String.format("Teleport complete for %s (session %s) at %s",
                        ownerName, sessionId, formatLocation()));
    }

    private static String formatLoc(Location l) {
        if (l == null || l.getWorld() == null) return "null";
        return String.format("%s(%.1f, %.1f, %.1f)", l.getWorld().getName(), l.getX(), l.getY(), l.getZ());
    }

    /**
     * Re-sends player info and entity spawn packets to all online players to refresh the skin and model.
     */
    private void resendPlayerInfoToAll() {
        if (!spawned || closed) return;
        Player dummy = handle.getBukkitEntity();
        for (Player viewer : java.util.List.copyOf(dummy.getTrackedBy())) {
            if (!viewer.equals(dummy) && viewer.canSee(dummy)) {
                viewer.hidePlayer(plugin, dummy);
                viewer.showPlayer(plugin, dummy);
            }
        }
    }

    /**
     * Cleanly removes the dummy from the server.
     */
    public void remove() {
        if (closed) return;
        closed = true;
        try {
            removeScoreboardTeam();
            if (registrationAttempted) {
                MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
                server.getPlayerList().remove(handle);
            }
        } finally {
            spawned = false;
            if (connection.channel instanceof EmbeddedChannel channel) channel.finishAndReleaseAll();
            deletePlayerData(handle.getUUID());
        }
    }



    /**
     * Sends spawn packets to a specific player (e.g. on join).
     */
    public void sendSpawnPacketsTo(Player player) {
        // Intentionally empty: Paper sends player info before tracking a player entity.
    }

    /**
     * Generates a unique, deterministic UUID for each dummy session.
     */
    private static UUID generateDummyUUID(UUID ownerUUID, UUID sessionId) {
        return UUID.nameUUIDFromBytes(("afkdummy:" + ownerUUID + ":" + sessionId).getBytes());
    }

    /**
     * Cleans and sanitizes a raw dummy name by stripping any existing AFK prefix.
     */
    public static String sanitizeRawName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Dummy";
        }
        String trimmed = name.trim();
        while (trimmed.toUpperCase().startsWith("[AFK]") || trimmed.toUpperCase().startsWith("AFK_") || trimmed.toUpperCase().startsWith("AFK ")) {
            if (trimmed.toUpperCase().startsWith("[AFK]")) {
                trimmed = trimmed.substring(5).trim();
            } else if (trimmed.toUpperCase().startsWith("AFK_")) {
                trimmed = trimmed.substring(4).trim();
            } else if (trimmed.toUpperCase().startsWith("AFK ")) {
                trimmed = trimmed.substring(4).trim();
            }
        }
        if (trimmed.isEmpty()) {
            trimmed = "Dummy";
        }
        return trimmed;
    }

    /**
     * Formats the authoritative display name for TAB and nametag display: "[AFK] <cleanName>".
     */
    public static String formatDisplayName(String rawName) {
        String clean = sanitizeRawName(rawName);
        return "[AFK] " + clean;
    }

    /**
     * Gets this dummy's clean raw name (e.g. "Afkin" or "Steve" or ownerName).
     */
    public String getRawName() {
        return customName != null ? customName : ownerName;
    }

    /**
     * Gets this dummy's formatted display name (e.g. "[AFK] Afkin" or "[AFK] JustRyt").
     */
    public String getFormattedDisplayName() {
        return formatDisplayName(getRawName());
    }

    /**
     * Generates a valid alphanumeric GameProfile username (<= 16 chars) with zero trailing suffixes or extra characters.
     */
    public static String generateProfileName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Dummy";
        }
        String clean = sanitizeRawName(name);
        String sanitized = clean.replaceAll("[^a-zA-Z0-9_]", "");
        if (sanitized.isEmpty()) {
            sanitized = "Dummy";
        }
        if (sanitized.length() > 16) {
            sanitized = sanitized.substring(0, 16);
        }
        return sanitized;
    }

    /**
     * Overload for backward compatibility.
     */
    public static String generateProfileName(String name, UUID sessionId) {
        return "AFK_" + sessionId.toString().replace("-", "").substring(0, 12);
    }

    /**
     * Formats the dummy's location as a readable string.
     */
    private String formatLocation() {
        return String.format("%s (%.1f, %.1f, %.1f)",
                ((ServerLevel) handle.level()).getWorld().getName(),
                handle.getX(), handle.getY(), handle.getZ());
    }

    // ========================================================================
    // Accessors
    // ========================================================================

    /** @return the unique session ID */
    public UUID getSessionId() {
        return sessionId;
    }

    /** @return the underlying NMS ServerPlayer handle */
    public ServerPlayer getHandle() {
        return handle;
    }

    /** @return the UUID of the player who owns this dummy */
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    /** @return the display name of the owner */
    public String getOwnerName() {
        return ownerName;
    }

    /** @return the NMS entity ID */
    public int getEntityId() {
        return handle.getId();
    }

    /** @return true if the dummy is currently spawned in the world */
    public boolean isSpawned() {
        return spawned;
    }

    /** @return the dummy's current Location as a Bukkit Location */
    public Location getLocation() {
        return handle.getBukkitEntity().getLocation();
    }

    /** @return the Bukkit Player entity wrapping this dummy */
    public Player getBukkitPlayer() {
        return handle.getBukkitEntity();
    }
}
