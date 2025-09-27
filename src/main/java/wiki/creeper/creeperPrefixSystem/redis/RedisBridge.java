package wiki.creeper.creeperPrefixSystem.redis;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import wiki.creeper.creeperPrefixSystem.config.RedisConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles Redis pub/sub interactions for synchronising state between network nodes.
 */
public final class RedisBridge implements AutoCloseable {

    private final Logger logger;
    private final RedisConfig config;
    private JedisPool pool;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "TitlePlus-RedisSubscriber"));
    private Future<?> subscriberTask;
    private final Map<String, RedisMessageListener> listeners = new ConcurrentHashMap<>();
    private final Map<String, StructuredRedisListener> structuredListeners = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();

    public RedisBridge(Logger logger, RedisConfig config) {
        this.logger = logger;
        this.config = config;
    }

    public void connect() {
        if (!config.enabled()) {
            return;
        }
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        String username = normalize(config.username());
        String password = normalize(config.password());
        if (username != null) {
            pool = new JedisPool(poolConfig, config.host(), config.port(), 2000, username, password);
        } else {
            pool = new JedisPool(poolConfig, config.host(), config.port(), 2000, password);
        }
        subscriberTask = executor.submit(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        listeners.values().forEach(listener -> listener.onMessage(channel, message));
                        structuredListeners.values().forEach(listener -> {
                            try {
                                JsonObject json = GSON.fromJson(message, JsonObject.class);
                                if (json != null && json.has("type")) {
                                    listener.onMessage(channel, json);
                                }
                            } catch (JsonSyntaxException ignored) {
                            }
                        });
                    }
                }, config.broadcastChannel(), config.apiRequestChannel());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Redis subscription loop exited", ex);
            }
        });
        logger.info("Connected to Redis " + config.host() + ":" + config.port());
    }

    public void addListener(String id, RedisMessageListener listener) {
        listeners.put(id, listener);
    }

    public void removeListener(String id) {
        listeners.remove(id);
        structuredListeners.remove(id);
    }

    public void publishBroadcast(String payload) {
        publish(config.broadcastChannel(), payload);
    }

    public void publishBroadcast(JsonObject payload) {
        publishStructured(config.broadcastChannel(), payload);
    }

    public void publish(String channel, String payload) {
        if (!config.enabled() || pool == null) {
            return;
        }
        executor.execute(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.publish(channel, payload);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Failed to publish Redis message", ex);
            }
        });
    }

    public void publishStructured(String channel, JsonObject payload) {
        publish(channel, GSON.toJson(payload));
    }

    public void query(String requestPayload) {
        publish(config.apiRequestChannel(), requestPayload);
    }

    @Override
    public void close() {
        listeners.clear();
        if (subscriberTask != null) {
            subscriberTask.cancel(true);
        }
        executor.shutdownNow();
        if (pool != null) {
            pool.close();
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.isBlank() ? null : value;
    }

    @FunctionalInterface
    public interface RedisMessageListener {
        void onMessage(String channel, String message);
    }

    @FunctionalInterface
    public interface StructuredRedisListener {
        void onMessage(String channel, JsonObject message);
    }

    public void addStructuredListener(String id, StructuredRedisListener listener) {
        structuredListeners.put(id, listener);
    }
}
