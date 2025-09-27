package wiki.creeper.creeperPrefixSystem.config;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Aggregated configuration holder bridging Bukkit's {@link FileConfiguration} to strongly typed
 * value objects.
 */
public final class TitlePlusConfiguration {

    private final StorageConfig storageConfig;
    private final RedisConfig redisConfig;
    private final boolean debugLogging;
    private final long cacheExpireSeconds;
    private final long statSyncSeconds;
    private final long seasonSyncIntervalTicks;
    private final boolean seasonAutoSync;
    private final long weeklyEvaluationIntervalMinutes;
    private final String defaultWeeklyMetric;
    private final String nodeId;

    public TitlePlusConfiguration(StorageConfig storageConfig,
                                  RedisConfig redisConfig,
                                  boolean debugLogging,
                                  long cacheExpireSeconds,
                                  long statSyncSeconds,
                                  long seasonSyncIntervalTicks,
                                  boolean seasonAutoSync,
                                  long weeklyEvaluationIntervalMinutes,
                                  String defaultWeeklyMetric,
                                  String nodeId) {
        this.storageConfig = storageConfig;
        this.redisConfig = redisConfig;
        this.debugLogging = debugLogging;
        this.cacheExpireSeconds = cacheExpireSeconds;
        this.statSyncSeconds = statSyncSeconds;
        this.seasonSyncIntervalTicks = seasonSyncIntervalTicks;
        this.seasonAutoSync = seasonAutoSync;
        this.weeklyEvaluationIntervalMinutes = weeklyEvaluationIntervalMinutes;
        this.defaultWeeklyMetric = defaultWeeklyMetric;
        this.nodeId = nodeId;
    }

    public StorageConfig storage() {
        return storageConfig;
    }

    public RedisConfig redis() {
        return redisConfig;
    }

    public boolean debugLogging() {
        return debugLogging;
    }

    public long cacheExpireSeconds() {
        return cacheExpireSeconds;
    }

    public long statSyncSeconds() {
        return statSyncSeconds;
    }

    public long seasonSyncIntervalTicks() {
        return seasonSyncIntervalTicks;
    }

    public boolean seasonAutoSync() {
        return seasonAutoSync;
    }

    public long weeklyEvaluationIntervalMinutes() {
        return weeklyEvaluationIntervalMinutes;
    }

    public String defaultWeeklyMetric() {
        return defaultWeeklyMetric;
    }

    public String nodeId() {
        return nodeId;
    }

    public static TitlePlusConfiguration from(FileConfiguration config) {
        StorageConfig storage = new StorageConfig(
                envOr("TITLEPLUS_MYSQL_HOST", config.getString("mysql.host", "localhost")),
                (int) numberOr("TITLEPLUS_MYSQL_PORT", config.getInt("mysql.port", 3306)),
                envOr("TITLEPLUS_MYSQL_DATABASE", config.getString("mysql.database", "titleplus")),
                envOr("TITLEPLUS_MYSQL_USERNAME", config.getString("mysql.username", "root")),
                envOr("TITLEPLUS_MYSQL_PASSWORD", config.getString("mysql.password", "")),
                (int) numberOr("TITLEPLUS_MYSQL_POOL_MAX", config.getInt("mysql.pool.maximum-pool-size", 10)),
                (int) numberOr("TITLEPLUS_MYSQL_POOL_MIN", config.getInt("mysql.pool.minimum-idle", 2)),
                numberOr("TITLEPLUS_MYSQL_TIMEOUT", config.getLong("mysql.pool.connection-timeout", 30000L))
        );

        RedisConfig redis = new RedisConfig(
                Boolean.parseBoolean(envOr("TITLEPLUS_REDIS_ENABLED", String.valueOf(config.getBoolean("redis.enabled", false)))),
                envOr("TITLEPLUS_REDIS_HOST", config.getString("redis.host", "localhost")),
                (int) numberOr("TITLEPLUS_REDIS_PORT", config.getInt("redis.port", 6379)),
                envOr("TITLEPLUS_REDIS_USERNAME", config.getString("redis.username", null)),
                envOr("TITLEPLUS_REDIS_PASSWORD", config.getString("redis.password", null)),
                envOr("TITLEPLUS_REDIS_CHANNEL_BROADCAST", config.getString("redis.channels.broadcast", "tp.broadcast")),
                envOr("TITLEPLUS_REDIS_CHANNEL_REQUEST", config.getString("redis.channels.api-request", "tp.api.request")),
                envOr("TITLEPLUS_REDIS_CHANNEL_REPLY", config.getString("redis.channels.api-reply", "tp.api.reply")),
                envOr("TITLEPLUS_REDIS_KEY_SEASON", config.getString("redis.keys.season", "tp:season:current")),
                envOr("TITLEPLUS_REDIS_KEY_WEEK", config.getString("redis.keys.week", "tp:week:current")),
                envOr("TITLEPLUS_REDIS_KEY_WEEKLY_TOP", config.getString("redis.keys.weekly-top", "tp:rank:top3"))
        );

        boolean debug = config.getBoolean("logging.debug", false);
        long cacheExpire = config.getLong("cache.player-expire-seconds", 300L);
        long statSync = config.getLong("cache.stat-mod-sync-seconds", 60L);
        long seasonSyncTicks = config.getLong("season.sync-interval-ticks", 6000L);
        boolean seasonAuto = Boolean.parseBoolean(envOr("TITLEPLUS_SEASON_AUTO", String.valueOf(config.getBoolean("season.auto-sync", true))));
        long weeklyEval = numberOr("TITLEPLUS_WEEKLY_EVAL_MIN", config.getLong("weekly.evaluation-interval-minutes", 5L));
        String defaultMetric = envOr("TITLEPLUS_WEEKLY_METRIC", config.getString("weekly.metrics.default", "FARMING_POINTS"));
        String nodeId = envOr("TITLEPLUS_NODE_ID", config.getString("network.node-id", "paper-node"));

        return new TitlePlusConfiguration(storage, redis, debug, cacheExpire, statSync, seasonSyncTicks, seasonAuto, weeklyEval, defaultMetric, nodeId);
    }

    private static String envOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static long numberOr(String key, long fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
