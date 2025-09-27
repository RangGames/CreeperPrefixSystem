package wiki.creeper.creeperPrefixSystem.service;

import wiki.creeper.creeperPrefixSystem.data.ranking.WeeklyStanding;
import wiki.creeper.creeperPrefixSystem.redis.RedisBridge;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Proxy authority for weekly leaderboard evaluation.
 */
public final class ProxyWeeklyCoordinator {

    private final Logger logger;
    private final MySqlStorage storage;
    private final RedisBridge redis;
    private final ExecutorService executor;
    private final AtomicReference<String> weekKey;
    private final Map<String, List<WeeklyStanding>> cache = new ConcurrentHashMap<>();
    private final String defaultMetric;

    public ProxyWeeklyCoordinator(Logger logger,
                                  MySqlStorage storage,
                                  RedisBridge redis,
                                  ExecutorService executor,
                                  String defaultMetric) {
        this.logger = logger;
        this.storage = storage;
        this.redis = redis;
        this.executor = executor;
        this.defaultMetric = defaultMetric;
        this.weekKey = new AtomicReference<>(currentWeekKey());
    }

    public void refreshWeekKey() {
        weekKey.set(currentWeekKey());
    }

    public String getDefaultMetric() {
        return defaultMetric;
    }

    public CompletableFuture<List<WeeklyStanding>> evaluate(String metric) {
        String currentWeek = weekKey.get();
        return CompletableFuture.supplyAsync(() -> {
            List<WeeklyStanding> standings = storage.loadWeeklyStandings(currentWeek, metric, 10);
            cache.put(metric, standings);
            if (redis != null) {
                redis.publishBroadcast("weekly:" + metric + ":" + currentWeek);
            }
            logger.info("[TitlePlus-Proxy] Evaluated weekly metric " + metric + " for week " + currentWeek);
            return standings;
        }, executor);
    }

    public List<WeeklyStanding> getStandings(String metric) {
        return cache.getOrDefault(metric, Collections.emptyList());
    }

    private String currentWeekKey() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        WeekFields fields = WeekFields.ISO;
        int week = date.get(fields.weekOfWeekBasedYear());
        return String.format(Locale.ROOT, "%d%02d", date.getYear(), week);
    }
}
