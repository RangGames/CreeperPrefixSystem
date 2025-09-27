package wiki.creeper.creeperPrefixSystem.config;

/**
 * Configuration model for the Velocity proxy component.
 */
public record ProxyConfig(StorageConfig mysql,
                          RedisConfig redis,
                          boolean seasonAutoSync,
                          long seasonSyncIntervalSeconds,
                          long weeklyEvaluationMinutes,
                          String defaultWeeklyMetric) {

    public static ProxyConfig defaults() {
        StorageConfig storage = new StorageConfig("localhost", 3306, "titleplus", "root", "password", 10, 2, 30000L);
        RedisConfig redis = new RedisConfig(false, "localhost", 6379, null, null,
                "tp.broadcast", "tp.api.request", "tp.api.reply",
                "tp:season:current", "tp:week:current", "tp:rank:top3");
        return new ProxyConfig(storage, redis, true, 300L, 5L, "FARMING_POINTS");
    }
}
