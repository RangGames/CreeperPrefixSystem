package wiki.creeper.creeperPrefixSystem.service;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import wiki.creeper.creeperPrefixSystem.TitlePlusPlugin;
import wiki.creeper.creeperPrefixSystem.data.collection.CollectionEntry;
import wiki.creeper.creeperPrefixSystem.data.collection.PlayerCollectionState;
import wiki.creeper.creeperPrefixSystem.event.CollectionRegisterEvent;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;
import wiki.creeper.creeperPrefixSystem.util.EventDispatcher;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles 도감 등록 로직, 저장 및 경험치 보상 처리.
 */
public final class CollectionService {

    private static final String DROPPED_METADATA_KEY = "collection-player-drop";
    private static final double REGISTER_CHANCE = 0.005; // 0.5%
    private static final int BASE_XP = 100;

    private final TitlePlusPlugin plugin;
    private final MySqlStorage storage;
    private final AchievementService achievementService;
    private final ExecutorService executor;
    private final Map<UUID, PlayerCollectionState> cache = new ConcurrentHashMap<>();

    public CollectionService(TitlePlusPlugin plugin,
                             MySqlStorage storage,
                             AchievementService achievementService,
                             ExecutorService executor) {
        this.plugin = plugin;
        this.storage = storage;
        this.achievementService = achievementService;
        this.executor = executor;
    }

    public CompletableFuture<PlayerCollectionState> load(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerCollectionState state = new PlayerCollectionState();
            storage.loadCollectionEntries(uuid).forEach(state::addEntry);
            cache.put(uuid, state);
            return state;
        }, executor);
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public void markPlayerDrop(Item item) {
        item.setMetadata(DROPPED_METADATA_KEY, new FixedMetadataValue(plugin, Boolean.TRUE));
    }

    public boolean shouldConsider(Item item) {
        if (item.getItemStack().getType().isAir()) {
            return false;
        }
        for (MetadataValue metadata : item.getMetadata(DROPPED_METADATA_KEY)) {
            if (metadata.getOwningPlugin() == plugin && metadata.asBoolean()) {
                return false;
            }
        }
        return true;
    }

    public void handleNaturalPickup(Player player, Material material) {
        if (material.isAir()) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() >= REGISTER_CHANCE) {
            return;
        }
        registerEntry(player.getUniqueId(), material, player, true, true);
    }

    public boolean registerEntry(UUID uuid, Material material) {
        return registerEntry(uuid, material, Bukkit.getPlayer(uuid), true, true);
    }

    public boolean registerEntry(UUID uuid, Material material, boolean grantXp, boolean notify) {
        Player player = Bukkit.getPlayer(uuid);
        return registerEntry(uuid, material, player, grantXp, notify);
    }

    public Collection<CollectionEntry> getEntries(UUID uuid) {
        return getOrLoadSync(uuid).getEntries();
    }

    public boolean hasEntry(UUID uuid, Material material) {
        return getOrLoadSync(uuid).hasEntry(material);
    }

    public int getEntryCount(UUID uuid) {
        return getOrLoadSync(uuid).size();
    }

    private boolean registerEntry(UUID uuid, Material material, Player context, boolean grantXp, boolean notify) {
        if (material == null || material.isAir()) {
            return false;
        }
        PlayerCollectionState state = cache.get(uuid);
        if (state == null) {
            state = getOrLoadSync(uuid);
        }
        if (state.hasEntry(material)) {
            return false;
        }

        int previousCount = state.size();
        int rank = previousCount + 1;
        int xpReward = grantXp ? calculateXp(previousCount) : 0;

        CollectionRegisterEvent event = EventDispatcher.dispatch(plugin,
                new CollectionRegisterEvent(uuid, material, previousCount, rank, xpReward, grantXp, !Bukkit.isPrimaryThread()));
        if (event.isCancelled()) {
            return false;
        }

        rank = event.getPlayerRank();
        xpReward = event.getXpReward();
        grantXp = event.shouldGrantXp();

        Instant now = Instant.now();
        CollectionEntry entry = new CollectionEntry(material, now, rank, -1);
        state.addEntry(entry);

        Player player = context != null ? context : Bukkit.getPlayer(uuid);
        String display = formatMaterialName(material.name());
        boolean announce = notify && event.shouldAnnounce();

        if (announce && player != null) {
            player.sendMessage("§6도감 등록! §f" + display + " §7(#" + rank + ")");
        }
        if (grantXp && xpReward > 0 && player != null) {
            player.giveExp(xpReward);
            if (announce) {
                player.sendMessage("§a+" + xpReward + " 경험치");
            }
        }

        achievementService.handleCollectionCount(uuid, state.size());

        final boolean shouldNotify = announce;
        executor.execute(() -> {
            long globalRank = storage.insertCollectionEntry(uuid, entry);
            if (globalRank > 0) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    entry.setGlobalRank(globalRank);
                    if (!shouldNotify) {
                        return;
                    }
                    Player online = player != null && player.isOnline() ? player : Bukkit.getPlayer(uuid);
                    if (online != null && online.isOnline()) {
                        online.sendMessage("§7도감 등록 순위: §e#" + globalRank);
                    }
                });
            }
        });
        return true;
    }

    private String formatMaterialName(String material) {
        String[] parts = material.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private PlayerCollectionState getOrLoadSync(UUID uuid) {
        return cache.computeIfAbsent(uuid, key -> {
            PlayerCollectionState state = new PlayerCollectionState();
            storage.loadCollectionEntries(uuid).forEach(state::addEntry);
            return state;
        });
    }

    private int calculateXp(int ownedBefore) {
        return BASE_XP * (ownedBefore == 0 ? 1 : ownedBefore);
    }

    public String getDisplayName(Material material) {
        return formatMaterialName(material.name());
    }
}
