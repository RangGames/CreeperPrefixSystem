package wiki.creeper.creeperPrefixSystem.service;

import wiki.creeper.creeperPrefixSystem.data.season.SeasonSnapshot;
import wiki.creeper.creeperPrefixSystem.data.season.SeasonState;
import wiki.creeper.creeperPrefixSystem.redis.RedisBridge;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Proxy-side authority for season state.
 */
public final class ProxySeasonCoordinator {

    private final Logger logger;
    private final MySqlStorage storage;
    private final RedisBridge redis;
    private final ExecutorService executor;
    private final AtomicReference<SeasonSnapshot> current = new AtomicReference<>();

    public ProxySeasonCoordinator(Logger logger,
                                  MySqlStorage storage,
                                  RedisBridge redis,
                                  ExecutorService executor) {
        this.logger = logger;
        this.storage = storage;
        this.redis = redis;
        this.executor = executor;
    }

    public void init() {
        executor.execute(() -> {
            SeasonSnapshot snapshot = storage.loadLatestSeason();
            if (snapshot != null) {
                current.set(snapshot);
                logger.info("[TitlePlus-Proxy] Loaded season " + snapshot.name() + " (" + snapshot.state() + ")");
            } else {
                logger.warning("[TitlePlus-Proxy] No season found in database; defaulting to PREPARING");
                SeasonSnapshot created = storage.createSeason("PreSeason", SeasonState.PREPARING);
                current.set(created != null ? created : new SeasonSnapshot(0, "PreSeason", null, null, SeasonState.PREPARING));
            }
        });
    }

    public Optional<SeasonSnapshot> getSnapshot() {
        return Optional.ofNullable(current.get());
    }

    public SeasonState getState() {
        SeasonSnapshot snapshot = current.get();
        return snapshot == null ? SeasonState.PREPARING : snapshot.state();
    }

    public void refresh() {
        executor.execute(() -> {
            SeasonSnapshot snapshot = storage.loadLatestSeason();
            if (snapshot != null) {
                current.set(snapshot);
            }
        });
    }

    public void updateState(SeasonState newState) {
        SeasonSnapshot snapshot = current.get();
        if (snapshot == null || snapshot.state() == newState) {
            return;
        }
        SeasonSnapshot updated = new SeasonSnapshot(snapshot.id(), snapshot.name(), snapshot.startAt(), snapshot.endAt(), newState);
        current.set(updated);
        executor.execute(() -> storage.updateSeasonState(snapshot.id(), newState));
        if (redis != null) {
            redis.publishBroadcast("season:" + newState.name());
        }
        logger.info("[TitlePlus-Proxy] Season state updated to " + newState);
    }
}
