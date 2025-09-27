package wiki.creeper.creeperPrefixSystem.service;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import wiki.creeper.creeperPrefixSystem.data.player.PlayerProgressState;
import wiki.creeper.creeperPrefixSystem.data.title.TitleDefinition;
import wiki.creeper.creeperPrefixSystem.data.title.TitleRegistry;
import wiki.creeper.creeperPrefixSystem.data.title.TitleRequirement;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Tracks requirement progress for titles and evaluates unlock conditions in response to game actions.
 */
public final class RequirementService {

    private final TitleRegistry titleRegistry;
    private final MySqlStorage storage;
    private final TitleService titleService;
    private final WeeklyRankingService weeklyRankingService;
    private final ExecutorService executor;

    private final Map<UUID, PlayerProgressState> cache = new ConcurrentHashMap<>();
    private final Map<Material, List<TitleDefinition>> breakIndex = new ConcurrentHashMap<>();
    private final Map<Material, List<TitleDefinition>> sellIndex = new ConcurrentHashMap<>();

    public RequirementService(TitleRegistry titleRegistry,
                              MySqlStorage storage,
                              TitleService titleService,
                              WeeklyRankingService weeklyRankingService,
                              ExecutorService executor) {
        this.titleRegistry = titleRegistry;
        this.storage = storage;
        this.titleService = titleService;
        this.weeklyRankingService = weeklyRankingService;
        this.executor = executor;
        rebuildIndexes();
    }

    public void rebuildIndexes() {
        breakIndex.clear();
        sellIndex.clear();
        for (TitleDefinition definition : titleRegistry.all()) {
            TitleRequirement requirement = definition.getRequirement();
            if (requirement == null) {
                continue;
            }
            Material material = requirement.getMaterial();
            if (material == null) {
                continue;
            }
            if (requirement.getType() == TitleRequirement.Type.BREAK) {
                breakIndex.computeIfAbsent(material, key -> new ArrayList<>()).add(definition);
            } else if (requirement.getType() == TitleRequirement.Type.SELL) {
                sellIndex.computeIfAbsent(material, key -> new ArrayList<>()).add(definition);
            }
        }
    }

    public CompletableFuture<PlayerProgressState> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerProgressState state = new PlayerProgressState();
            Map<String, Long> rows = storage.loadTitleProgress(uuid);
            rows.forEach(state::setProgress);
            cache.put(uuid, state);
            return state;
        }, executor);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public void handleBlockBreak(Player player, Material material) {
        UUID uuid = player.getUniqueId();
        List<TitleDefinition> candidates = breakIndex.get(material);
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        PlayerProgressState state = cache.computeIfAbsent(uuid, id -> new PlayerProgressState());
        for (TitleDefinition definition : candidates) {
            if (titleService.getOwnedTitles(uuid).contains(definition.getId())) {
                continue;
            }
            TitleRequirement requirement = definition.getRequirement();
            long value = state.addProgress(definition.getId(), 1);
            if (requirement.getAmount() > 0 && value >= requirement.getAmount()) {
                titleService.grantTitle(uuid, definition.getId());
            }
            executor.execute(() -> storage.incrementTitleProgress(uuid, definition.getId(), 1));
        }
        weeklyRankingService.incrementMetric(uuid, weeklyRankingService.getDefaultMetric(), 1);
    }

    public void addProgress(UUID uuid, String titleId, long amount) {
        if (amount <= 0) {
            return;
        }
        PlayerProgressState state = cache.computeIfAbsent(uuid, id -> new PlayerProgressState());
        long value = state.addProgress(titleId, amount);
        executor.execute(() -> storage.setTitleProgress(uuid, titleId, value));
        TitleDefinition definition = titleRegistry.get(titleId);
        if (definition != null) {
            TitleRequirement requirement = definition.getRequirement();
            if (requirement != null && requirement.getAmount() > 0 && value >= requirement.getAmount()) {
                titleService.grantTitle(uuid, titleId);
            }
        }
    }

    public void handleSell(UUID uuid, Material material, long amount) {
        if (amount <= 0) {
            return;
        }
        List<TitleDefinition> candidates = sellIndex.get(material);
        if (candidates == null || candidates.isEmpty()) {
            return;
        }
        PlayerProgressState state = cache.computeIfAbsent(uuid, id -> new PlayerProgressState());
        for (TitleDefinition definition : candidates) {
            if (titleService.getOwnedTitles(uuid).contains(definition.getId())) {
                continue;
            }
            TitleRequirement requirement = definition.getRequirement();
            long value = state.addProgress(definition.getId(), amount);
            if (requirement.getAmount() > 0 && value >= requirement.getAmount()) {
                titleService.grantTitle(uuid, definition.getId());
            }
            long increment = amount;
            executor.execute(() -> storage.incrementTitleProgress(uuid, definition.getId(), increment));
        }
        weeklyRankingService.incrementMetric(uuid, weeklyRankingService.getDefaultMetric(), amount);
    }

    public long getProgress(UUID uuid, String titleId) {
        PlayerProgressState state = cache.get(uuid);
        return state == null ? 0L : state.getProgress(titleId);
    }
}
