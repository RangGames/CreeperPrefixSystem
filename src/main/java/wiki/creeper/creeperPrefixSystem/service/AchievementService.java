package wiki.creeper.creeperPrefixSystem.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import wiki.creeper.creeperPrefixSystem.TitlePlusPlugin;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementCompletion;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementDefinition;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementRegistry;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementType;
import wiki.creeper.creeperPrefixSystem.data.achievement.PlayerAchievementState;
import wiki.creeper.creeperPrefixSystem.event.AchievementUnlockEvent;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;
import wiki.creeper.creeperPrefixSystem.util.EventDispatcher;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Coordinates achievement tracking and unlock notifications.
 */
public final class AchievementService {

    private final TitlePlusPlugin plugin;
    private final MySqlStorage storage;
    private final AchievementRegistry registry;
    private final ExecutorService executor;
    private final Map<UUID, PlayerAchievementState> cache = new ConcurrentHashMap<>();

    public AchievementService(TitlePlusPlugin plugin,
                              MySqlStorage storage,
                              AchievementRegistry registry,
                              ExecutorService executor) {
        this.plugin = plugin;
        this.storage = storage;
        this.registry = registry;
        this.executor = executor;
    }

    public CompletableFuture<PlayerAchievementState> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerAchievementState state = new PlayerAchievementState();
            storage.loadAchievementCompletions(uuid).forEach(state::addCompletion);
            cache.put(uuid, state);
            return state;
        }, executor);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public void handleCollectionCount(UUID uuid, int collectionCount) {
        PlayerAchievementState state = cache.get(uuid);
        if (state == null) {
            state = getOrLoadSync(uuid);
        }
        List<AchievementDefinition> definitions = registry.getByType(AchievementType.COLLECTION_COUNT);
        if (definitions.isEmpty()) {
            return;
        }
        for (AchievementDefinition definition : definitions) {
            if (state.hasCompletion(definition.getId())) {
                continue;
            }
            if (definition.matchesCollectionCount(collectionCount)) {
                grantAchievement(uuid, definition, state);
            }
        }
    }

    private void grantAchievement(UUID uuid, AchievementDefinition definition, PlayerAchievementState state) {
        Instant now = Instant.now();
        int completionOrder = state.getCompletions().size() + 1;

        AchievementUnlockEvent event = EventDispatcher.dispatch(plugin,
                new AchievementUnlockEvent(uuid, definition, completionOrder, !Bukkit.isPrimaryThread()));
        if (event.isCancelled()) {
            return;
        }

        AchievementCompletion completion = new AchievementCompletion(definition.getId(), now, -1);
        state.addCompletion(completion);

        Player player = Bukkit.getPlayer(uuid);
        if (event.shouldAnnounce() && player != null && player.isOnline()) {
            player.sendMessage("§b업적 달성! §f" + definition.getDisplay());
            if (!definition.getDescription().isEmpty()) {
                player.sendMessage("§7" + definition.getDescription());
            }
        }

        executor.execute(() -> {
            long globalRank = storage.insertAchievementCompletion(uuid, completion);
            if (globalRank > 0) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    completion.setGlobalRank(globalRank);
                    Player online = Bukkit.getPlayer(uuid);
                    if (event.shouldAnnounce() && online != null && online.isOnline()) {
                        online.sendMessage("§7업적 달성 순위: §e#" + globalRank);
                    }
                });
            }
        });
    }

    public Collection<AchievementCompletion> getCompletions(UUID uuid) {
        return getOrLoadSync(uuid).getCompletions();
    }

    public boolean hasCompletion(UUID uuid, String achievementId) {
        return getOrLoadSync(uuid).hasCompletion(achievementId);
    }

    private PlayerAchievementState getOrLoadSync(UUID uuid) {
        return cache.computeIfAbsent(uuid, key -> {
            PlayerAchievementState state = new PlayerAchievementState();
            storage.loadAchievementCompletions(uuid).forEach(state::addCompletion);
            return state;
        });
    }
}
