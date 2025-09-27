package wiki.creeper.creeperPrefixSystem.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Handles reading and writing the Velocity configuration file.
 */
public final class ProxyConfigurationLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Logger logger;
    private final Path file;

    public ProxyConfigurationLoader(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.file = dataDirectory.resolve("titleplus-proxy.json");
    }

    public ProxyConfig load() {
        if (Files.notExists(file)) {
            try {
                Files.createDirectories(file.getParent());
                write(ProxyConfig.defaults());
            } catch (IOException ex) {
                logger.severe("Failed to create default proxy configuration: " + ex.getMessage());
            }
        }
        try (Reader reader = new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8)) {
            ProxyConfig config = GSON.fromJson(reader, ProxyConfig.class);
            return applyEnv(config == null ? ProxyConfig.defaults() : config);
        } catch (JsonIOException | JsonSyntaxException | IOException ex) {
            logger.severe("Failed to read proxy configuration: " + ex.getMessage());
            return ProxyConfig.defaults();
        }
    }

    public void write(ProxyConfig config) {
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(config, writer);
        } catch (IOException ex) {
            logger.severe("Failed to save proxy configuration: " + ex.getMessage());
        }
    }

    private ProxyConfig applyEnv(ProxyConfig config) {
        StorageConfig storage = new StorageConfig(
                envOr("TITLEPLUS_MYSQL_HOST", config.mysql().host()),
                (int) numberOr("TITLEPLUS_MYSQL_PORT", config.mysql().port()),
                envOr("TITLEPLUS_MYSQL_DATABASE", config.mysql().database()),
                envOr("TITLEPLUS_MYSQL_USERNAME", config.mysql().username()),
                envOr("TITLEPLUS_MYSQL_PASSWORD", config.mysql().password()),
                (int) numberOr("TITLEPLUS_MYSQL_POOL_MAX", config.mysql().maximumPoolSize()),
                (int) numberOr("TITLEPLUS_MYSQL_POOL_MIN", config.mysql().minimumIdle()),
                numberOr("TITLEPLUS_MYSQL_TIMEOUT", config.mysql().connectionTimeout())
        );

        RedisConfig redis = new RedisConfig(
                Boolean.parseBoolean(envOr("TITLEPLUS_REDIS_ENABLED", String.valueOf(config.redis().enabled()))),
                envOr("TITLEPLUS_REDIS_HOST", config.redis().host()),
                (int) numberOr("TITLEPLUS_REDIS_PORT", config.redis().port()),
                envOr("TITLEPLUS_REDIS_USERNAME", config.redis().username()),
                envOr("TITLEPLUS_REDIS_PASSWORD", config.redis().password()),
                envOr("TITLEPLUS_REDIS_CHANNEL_BROADCAST", config.redis().broadcastChannel()),
                envOr("TITLEPLUS_REDIS_CHANNEL_REQUEST", config.redis().apiRequestChannel()),
                envOr("TITLEPLUS_REDIS_CHANNEL_REPLY", config.redis().apiReplyChannel()),
                envOr("TITLEPLUS_REDIS_KEY_SEASON", config.redis().seasonKey()),
                envOr("TITLEPLUS_REDIS_KEY_WEEK", config.redis().weekKey()),
                envOr("TITLEPLUS_REDIS_KEY_WEEKLY_TOP", config.redis().weeklyTopPrefix())
        );

        boolean autoSync = Boolean.parseBoolean(envOr("TITLEPLUS_SEASON_AUTO", String.valueOf(config.seasonAutoSync())));
        long syncInterval = numberOr("TITLEPLUS_SEASON_SYNC_SECONDS", config.seasonSyncIntervalSeconds());
        long weeklyMinutes = numberOr("TITLEPLUS_WEEKLY_EVAL_MIN", config.weeklyEvaluationMinutes());
        String metric = envOr("TITLEPLUS_WEEKLY_METRIC", config.defaultWeeklyMetric());

        return new ProxyConfig(storage, redis, autoSync, syncInterval, weeklyMinutes, metric);
    }

    private String envOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private long numberOr(String key, long fallback) {
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
