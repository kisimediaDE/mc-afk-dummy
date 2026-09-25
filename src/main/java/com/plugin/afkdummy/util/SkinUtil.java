package com.plugin.afkdummy.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Utility class for fetching and applying Minecraft player skins
 * from the Mojang session server API.
 * <p>
 * Skin data is cached in memory to avoid redundant API calls.
 * All network operations run asynchronously off the main server thread.
 * </p>
 */
public final class SkinUtil {

    private static final String SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private static final ConcurrentHashMap<UUID, Property> SKIN_CACHE = new ConcurrentHashMap<>();

    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final ConcurrentHashMap<String, UUID> NAME_TO_UUID_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, java.util.concurrent.CompletableFuture<Property>> SKIN_REQUESTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, java.util.concurrent.CompletableFuture<UUID>> NAME_REQUESTS = new ConcurrentHashMap<>();
    private static final java.util.concurrent.Semaphore REQUEST_SLOTS = new java.util.concurrent.Semaphore(8);

    /** All callbacks that can touch entities are delivered on the server thread. */
    private static <T> void deliver(Plugin plugin, Consumer<T> callback, T value) {
        if (!plugin.isEnabled()) return;
        if (Bukkit.isPrimaryThread()) callback.accept(value);
        else Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.isEnabled()) callback.accept(value);
        });
    }

    static <K, V> void request(K key, ConcurrentHashMap<K, V> cache,
            ConcurrentHashMap<K, java.util.concurrent.CompletableFuture<V>> requests,
            java.util.function.Supplier<V> fetch, Consumer<V> callback, Plugin plugin) {
        if (!plugin.isEnabled()) return;
        V cached = cache.get(key);
        if (cached != null) {
            deliver(plugin, callback, cached);
            return;
        }
        var future = new java.util.concurrent.CompletableFuture<V>();
        var existing = requests.putIfAbsent(key, future);
        (existing != null ? existing : future).thenAccept(value -> deliver(plugin, callback, value));
        if (existing != null) return;
        // Bound blocking HTTP work, including bursts of distinct skin names.
        if (!REQUEST_SLOTS.tryAcquire()) {
            requests.remove(key, future);
            future.complete(null);
            return;
        }
        try {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                V result = null;
                try {
                    result = fetch.get();
                    if (result != null && plugin.isEnabled()) {
                        synchronized (cache) {
                            if (cache.size() >= 256) cache.remove(cache.keys().nextElement());
                            cache.put(key, result);
                        }
                    }
                } finally {
                    requests.remove(key, future);
                    REQUEST_SLOTS.release();
                    future.complete(result);
                }
            });
        } catch (RuntimeException e) {
            requests.remove(key, future);
            REQUEST_SLOTS.release();
            future.complete(null);
            throw e;
        }
    }

    private SkinUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Asynchronously fetches skin data for a player by their Minecraft username.
     *
     * @param username the Minecraft username
     * @param callback consumer that receives the skin Property, or null if fetch failed
     * @param plugin   the owning plugin instance for scheduling
     */
    public static void fetchSkinByNameAsync(String username, Consumer<Property> callback, Plugin plugin) {
        if (username == null || username.trim().isEmpty()) {
            if (plugin == null) callback.accept(null);
            else deliver(plugin, callback, null);
            return;
        }

        String name = username.trim().toLowerCase(java.util.Locale.ROOT);
        if (!name.matches("[a-z0-9_]{1,16}")) {
            deliver(plugin, callback, null);
            return;
        }
        request(name, NAME_TO_UUID_CACHE, NAME_REQUESTS, () -> resolveUUIDByUsername(name, plugin), resolved -> {
            if (resolved != null) {
                fetchSkinAsync(resolved, callback, plugin);
            } else {
                callback.accept(null);
            }
        }, plugin);
    }

    /**
     * Blocking call to resolve username to UUID via Mojang API.
     */
    private static UUID resolveUUIDByUsername(String username, Plugin plugin) {
        try {
            String url = String.format(PROFILE_URL, username.trim());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                if (body != null && body.trim().startsWith("{")) {
                    JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                    if (json.has("id")) {
                        String idStr = json.get("id").getAsString();
                        // Format UUID with dashes (8-4-4-4-12)
                        String formatted = idStr.replaceFirst(
                                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                                "$1-$2-$3-$4-$5"
                        );
                        return UUID.fromString(formatted);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to resolve UUID for username " + username + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Asynchronously fetches skin data for a player from Mojang's session server.
     * Results are cached for subsequent calls.
     *
     * @param playerUUID the UUID of the player whose skin to fetch
     * @param callback   consumer that receives the skin Property, or null if fetch failed
     * @param plugin     the owning plugin instance for scheduling
     */
    public static void fetchSkinAsync(UUID playerUUID, Consumer<Property> callback, Plugin plugin) {
        request(playerUUID, SKIN_CACHE, SKIN_REQUESTS, () -> fetchSkinBlocking(playerUUID, plugin), callback, plugin);
    }

    /**
     * Blocking skin fetch from Mojang API. Should only be called from async context.
     */
    private static Property fetchSkinBlocking(UUID playerUUID, Plugin plugin) {
        try {
            String uuidNoDashes = playerUUID.toString().replace("-", "");
            String url = String.format(SESSION_URL, uuidNoDashes);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 429) {
                plugin.getLogger().warning("Mojang API rate limit hit while fetching skin for " + playerUUID + ". Retrying later.");
                return null;
            }

            if (response.statusCode() != 200) {
                plugin.getLogger().warning("Mojang API returned status " + response.statusCode()
                        + " for UUID " + playerUUID);
                return null;
            }

            String body = response.body();
            if (body == null || !body.trim().startsWith("{")) {
                plugin.getLogger().warning("Mojang API returned invalid response for UUID " + playerUUID);
                return null;
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("properties") || !json.get("properties").isJsonArray()) {
                plugin.getLogger().warning("No properties array found in Mojang response for " + playerUUID);
                return null;
            }

            JsonArray properties = json.getAsJsonArray("properties");
            for (JsonElement element : properties) {
                if (!element.isJsonObject()) continue;
                JsonObject prop = element.getAsJsonObject();
                if (prop.has("name") && "textures".equals(prop.get("name").getAsString())) {
                    String value = prop.has("value") ? prop.get("value").getAsString() : "";
                    String signature = prop.has("signature")
                            ? prop.get("signature").getAsString()
                            : "";
                    return new Property("textures", value, signature);
                }
            }

            plugin.getLogger().warning("No textures property found for UUID " + playerUUID);
            return null;

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to fetch skin for UUID " + playerUUID + ": " + e.getMessage(), e);
            return null;
        }
    }

    /** Return an independent immutable profile; never mutate PropertyMap.EMPTY. */
    public static GameProfile applySkin(GameProfile profile, Property textures) {
        if (profile == null || textures == null) return profile;
        var properties = com.google.common.collect.ImmutableMultimap.<String, Property>builder();
        profile.properties().entries().stream().filter(e -> !e.getKey().equals("textures"))
                .forEach(e -> properties.put(e.getKey(), e.getValue()));
        properties.put("textures", textures);
        return new GameProfile(profile.id(), profile.name(),
                new com.mojang.authlib.properties.PropertyMap(properties.build()));
    }

    /**
     * Clears the internal skin cache.
     */
    public static void clearCache() {
        synchronized (SKIN_CACHE) { SKIN_CACHE.clear(); }
        synchronized (NAME_TO_UUID_CACHE) { NAME_TO_UUID_CACHE.clear(); }
    }
}
