package wiki.creeper.creeperPrefixSystem.service;

import com.velocitypowered.api.proxy.ProxyServer;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Resolves player names from UUIDs with a simple in-memory cache backed by Velocity's profile manager.
 */
public final class PlayerNameCache {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    private final ProxyServer proxy;
    private final Logger logger;
    private final Duration ttl;
    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    public PlayerNameCache(ProxyServer proxy, Logger logger) {
        this(proxy, logger, DEFAULT_TTL);
    }

    public PlayerNameCache(ProxyServer proxy, Logger logger, Duration ttl) {
        this.proxy = proxy;
        this.logger = logger;
        this.ttl = ttl;
    }

    public CompletableFuture<String> lookup(UUID uuid) {
        CacheEntry entry = cache.get(uuid);
        long now = System.currentTimeMillis();
        if (entry != null && entry.expiresAt() > now) {
            return CompletableFuture.completedFuture(entry.name());
        }
        return proxy.getPlayer(uuid)
                .map(player -> {
                    String name = player.getUsername();
                    cache.put(uuid, new CacheEntry(name, now + ttl.toMillis()));
                    return CompletableFuture.completedFuture(name);
                })
                .orElseGet(() -> CompletableFuture.completedFuture(shortUuid(uuid)));
    }

    private String shortUuid(UUID uuid) {
        return uuid.toString().split("-")[0];
    }

    private record CacheEntry(String name, long expiresAt) {
    }
}
