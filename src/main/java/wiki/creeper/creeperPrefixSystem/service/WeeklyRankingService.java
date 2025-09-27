package wiki.creeper.creeperPrefixSystem.service;

import org.bukkit.plugin.Plugin;
import wiki.creeper.creeperPrefixSystem.config.TitlePlusConfiguration;
import wiki.creeper.creeperPrefixSystem.data.ranking.WeeklyStanding;
import wiki.creeper.creeperPrefixSystem.event.WeeklyRankEvaluateEvent;
import wiki.creeper.creeperPrefixSystem.redis.RedisBridge;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;
import wiki.creeper.creeperPrefixSystem.util.EventDispatcher;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Tracks weekly activity metrics and computes leaderboard standings.
 */
public final class WeeklyRankingService {

    private static final DateTimeFormatter WEEK_FORMATTER = DateTimeFormatter.ofPattern("YYYYww");

    private final Plugin plugin;
    private final MySqlStorage storage;
    private final RedisBridge redis;
    private final ExecutorService executor;
    private final TitlePlusConfiguration configuration;
    private final String defaultMetric;

    private volatile String weekKey = currentWeekKey();
    private final Map<String, List<WeeklyStanding>> cache = new ConcurrentHashMap<>();
    private final Set<UUID> weeklyTop3 = ConcurrentHashMap.newKeySet();

    public WeeklyRankingService(Plugin plugin,
                                MySqlStorage storage,
                                RedisBridge redis,
                                ExecutorService executor,
                                TitlePlusConfiguration configuration) {
        this.plugin = plugin;
        this.storage = storage;
        this.redis = redis;
        this.executor = executor;
        this.configuration = configuration;
        this.defaultMetric = configuration.defaultWeeklyMetric();
    }

    public void refreshWeekKey() {
        weekKey = currentWeekKey();
    }

    public void incrementMetric(UUID uuid, String metric, long delta) {
        if (delta == 0) {
            return;
        }
        String currentWeek = weekKey;
        executor.execute(() -> storage.incrementWeeklyMetric(currentWeek, uuid, metric, delta));
    }

    public CompletableFuture<List<WeeklyStanding>> evaluate(String metric) {
        String currentWeek = weekKey;
        return CompletableFuture.supplyAsync(() -> {
            List<WeeklyStanding> standings = storage.loadWeeklyStandings(currentWeek, metric, 10);
            WeeklyRankEvaluateEvent event = EventDispatcher.dispatch(plugin,
                    new WeeklyRankEvaluateEvent(metric, currentWeek, new ArrayList<>(standings), !org.bukkit.Bukkit.isPrimaryThread()));
            List<WeeklyStanding> processed = new ArrayList<>(event.getStandings());
            cache.put(metric, Collections.unmodifiableList(processed));
            weeklyTop3.clear();
            processed.stream().limit(3).forEach(entry -> weeklyTop3.add(entry.getPlayerId()));
            if (configuration.redis().enabled()) {
                redis.publishBroadcast("weekly:" + metric + ":" + currentWeek);
            }
            return Collections.unmodifiableList(processed);
        }, executor);
    }

    public String getDefaultMetric() {
        return defaultMetric;
    }

    public List<WeeklyStanding> getCachedStandings(String metric) {
        return cache.getOrDefault(metric, Collections.emptyList());
    }

    public boolean isTop3(UUID uuid) {
        return weeklyTop3.contains(uuid);
    }

    public Set<String> getCachedMetrics() {
        return cache.keySet();
    }

    private String currentWeekKey() {
        LocalDate date = LocalDate.now(ZoneOffset.UTC);
        WeekFields fields = WeekFields.ISO;
        int week = date.get(fields.weekOfWeekBasedYear());
        return String.format(Locale.ROOT, "%d%02d", date.getYear(), week);
    }
}
