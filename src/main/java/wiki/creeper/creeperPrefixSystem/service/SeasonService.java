package wiki.creeper.creeperPrefixSystem.service;

import wiki.creeper.creeperPrefixSystem.config.TitlePlusConfiguration;
import wiki.creeper.creeperPrefixSystem.data.season.SeasonSnapshot;
import wiki.creeper.creeperPrefixSystem.data.season.SeasonState;
import wiki.creeper.creeperPrefixSystem.redis.RedisBridge;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Coordinates season state persistence and cross-server propagation.
 */
public final class SeasonService {

    private final Logger logger;
    private final MySqlStorage storage;
    private final RedisBridge redis;
    private final ExecutorService executor;
    private final TitlePlusConfiguration configuration;

    private final AtomicReference<SeasonSnapshot> current = new AtomicReference<>();

    public SeasonService(Logger logger,
                         MySqlStorage storage,
                         RedisBridge redis,
                         ExecutorService executor,
                         TitlePlusConfiguration configuration) {
        this.logger = logger;
        this.storage = storage;
        this.redis = redis;
        this.executor = executor;
        this.configuration = configuration;
    }

    public void init() {
        executor.execute(() -> {
            SeasonSnapshot snapshot = storage.loadLatestSeason();
            if (snapshot != null) {
                current.set(snapshot);
                logger.info("Loaded season " + snapshot.name() + " (" + snapshot.state() + ")");
            } else {
                logger.warning("No season found in database; defaulting to PREPARING");
                SeasonSnapshot created = storage.createSeason("PreSeason", SeasonState.PREPARING);
                current.set(created != null ? created : new SeasonSnapshot(0, "PreSeason", null, null, SeasonState.PREPARING));
            }
        });
    }

    public SeasonState getState() {
        SeasonSnapshot snapshot = current.get();
        return snapshot == null ? SeasonState.PREPARING : snapshot.state();
    }

    public Optional<SeasonSnapshot> getSnapshot() {
        return Optional.ofNullable(current.get());
    }

    public void refreshFromDatabase() {
        executor.execute(() -> {
            SeasonSnapshot snapshot = storage.loadLatestSeason();
            if (snapshot != null) {
                current.set(snapshot);
            }
        });
    }

    public void setState(SeasonState newState) {
        SeasonSnapshot snapshot = current.get();
        if (snapshot == null) {
            return;
        }
        if (snapshot.state() == newState) {
            return;
        }
        SeasonSnapshot updated = new SeasonSnapshot(snapshot.id(), snapshot.name(), snapshot.startAt(), snapshot.endAt(), newState);
        current.set(updated);
        executor.execute(() -> storage.updateSeasonState(snapshot.id(), newState));
        if (configuration.redis().enabled()) {
            redis.publishBroadcast("season:" + newState.name());
        }
    }
}
