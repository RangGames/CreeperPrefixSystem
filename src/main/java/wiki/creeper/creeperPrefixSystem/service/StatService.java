package wiki.creeper.creeperPrefixSystem.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import wiki.creeper.creeperPrefixSystem.data.player.PlayerStatState;
import wiki.creeper.creeperPrefixSystem.data.stat.StatDefinition;
import wiki.creeper.creeperPrefixSystem.data.stat.StatModifier;
import wiki.creeper.creeperPrefixSystem.data.stat.StatRegistry;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Manages player stat base values and modifiers. Computations are cached per player and periodically
 * persisted to the database to keep network nodes consistent.
 */
public final class StatService {

    private final Plugin plugin;
    private final StatRegistry statRegistry;
    private final MySqlStorage storage;
    private final ExecutorService executor;
    private final Map<UUID, PlayerStatState> cache = new ConcurrentHashMap<>();
    private NetworkSyncService networkSync;

    public StatService(Plugin plugin, StatRegistry statRegistry, MySqlStorage storage, ExecutorService executor) {
        this.plugin = plugin;
        this.statRegistry = statRegistry;
        this.storage = storage;
        this.executor = executor;
    }

    public CompletableFuture<PlayerStatState> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerStatState state = new PlayerStatState();
            storage.loadPlayerStats(uuid, state);
            cache.put(uuid, state);
            return state;
        }, executor);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public double getStat(UUID uuid, String statId) {
        PlayerStatState state = cache.get(uuid);
        if (state == null) {
            state = new PlayerStatState();
            cache.put(uuid, state);
            storage.loadPlayerStats(uuid, state);
        }
        StatDefinition definition = statRegistry.get(statId);
        if (definition == null) {
            return 0.0D;
        }
        double value = state.getBaseValue(statId);
        if (value == 0.0D) {
            value = definition.getDefaultValue();
        }

        double additive = 0.0D;
        double multiplier = 1.0D;
        Double setOverride = null;
        Collection<StatModifier> modifiers = state.getModifiers(statId);
        Instant now = Instant.now();
        for (StatModifier modifier : modifiers) {
            if (modifier.isExpired(now)) {
                removeModifierInternal(uuid, modifier.getStatId(), modifier.getSourceId(), true);
                continue;
            }
            switch (modifier.getOperation()) {
                case ADD -> additive += modifier.getValue();
                case MULT -> multiplier *= (1.0D + modifier.getValue());
                case SET -> setOverride = modifier.getValue();
            }
        }
        double computed = setOverride != null ? setOverride : (value + additive) * multiplier;
        return definition.clamp(computed);
    }

    public void setBaseStat(UUID uuid, String statId, double value) {
        PlayerStatState state = cache.computeIfAbsent(uuid, key -> new PlayerStatState());
        state.setBaseValue(statId, value);
        executor.execute(() -> storage.saveBaseStat(uuid, statId, value));
        broadcastBase(uuid, statId, value);
    }

    public void addModifier(UUID uuid,
                            String statId,
                            double value,
                            String sourceId,
                            StatModifier.Operation operation,
                            Long expireAt) {
        Instant expire = expireAt == null ? null : Instant.ofEpochMilli(expireAt);
        StatModifier modifier = new StatModifier(uuid, statId, sourceId, operation, value, expire);
        addModifierInternal(modifier, true, true);
    }

    public boolean removeModifier(UUID uuid, String statId, String sourceId) {
        return removeModifierInternal(uuid, statId, sourceId, true);
    }

    private void addModifierInternal(StatModifier modifier, boolean broadcast, boolean notifyPlayer) {
        PlayerStatState state = cache.computeIfAbsent(modifier.getPlayerId(), key -> new PlayerStatState());
        state.putModifier(modifier);
        executor.execute(() -> storage.upsertStatModifier(modifier));
        if (broadcast && networkSync != null) {
            networkSync.broadcastModifierAdd(new NetworkSyncService.StatModifierPayload(
                    modifier.getPlayerId(),
                    modifier.getStatId(),
                    modifier.getSourceId(),
                    modifier.getOperation().name(),
                    modifier.getValue(),
                    modifier.getExpireAt() == null ? null : modifier.getExpireAt().toEpochMilli()
            ));
        }
        if (notifyPlayer) {
            Player player = Bukkit.getPlayer(modifier.getPlayerId());
            if (player != null) {
                String message = "§aYour stat " + modifier.getStatId() + " was modified by " + modifier.getSourceId() + ".";
                Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(message));
            }
        }
    }

    private boolean removeModifierInternal(UUID uuid, String statId, String sourceId, boolean broadcast) {
        PlayerStatState state = cache.get(uuid);
        if (state == null) {
            return false;
        }
        boolean removed = state.removeModifier(statId, sourceId);
        if (removed) {
            executor.execute(() -> storage.removeStatModifier(uuid, statId, sourceId));
            if (broadcast && networkSync != null) {
                networkSync.broadcastModifierRemove(uuid, statId, sourceId);
            }
        }
        return removed;
    }

    private void broadcastBase(UUID uuid, String statId, double value) {
        if (networkSync != null) {
            networkSync.broadcastBase(uuid, statId, value);
        }
    }

    public void applyNetworkModifier(UUID uuid,
                                     String statId,
                                     String source,
                                     String operation,
                                     double value,
                                     Long expireAt) {
        Instant expire = expireAt == null ? null : Instant.ofEpochMilli(expireAt);
        StatModifier modifier = new StatModifier(uuid, statId, source, StatModifier.Operation.valueOf(operation), value, expire);
        addModifierInternal(modifier, false, false);
    }

    public void applyNetworkModifierRemoval(UUID uuid, String statId, String sourceId) {
        removeModifierInternal(uuid, statId, sourceId, false);
    }

    public void applyNetworkBase(UUID uuid, String statId, double value) {
        PlayerStatState state = cache.computeIfAbsent(uuid, key -> new PlayerStatState());
        state.setBaseValue(statId, value);
    }

    public void setNetworkSync(NetworkSyncService networkSync) {
        this.networkSync = networkSync;
    }
}
