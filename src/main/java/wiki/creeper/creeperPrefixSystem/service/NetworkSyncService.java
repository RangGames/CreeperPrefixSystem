package wiki.creeper.creeperPrefixSystem.service;

import com.google.gson.JsonObject;
import wiki.creeper.creeperPrefixSystem.redis.RedisBridge;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Coordinates stat synchronisation messages across Paper nodes via Redis.
 */
public final class NetworkSyncService implements RedisBridge.StructuredRedisListener {

    private final Logger logger;
    private final RedisBridge redis;
    private final StatService statService;
    private final String nodeId;
    private final boolean active;

    public NetworkSyncService(Logger logger,
                              RedisBridge redis,
                              StatService statService,
                              String nodeId,
                              boolean active) {
        this.logger = logger;
        this.redis = redis;
        this.statService = statService;
        this.nodeId = nodeId;
        this.active = active && redis != null;
    }

    public void init() {
        if (!active) {
            return;
        }
        redis.addStructuredListener("titleplus-network", this);
    }

    public void shutdown() {
        if (!active) {
            return;
        }
        redis.removeListener("titleplus-network");
    }

    public void broadcastModifierAdd(StatModifierPayload payload) {
        if (!active) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("type", "stat:modifier:add");
        json.addProperty("node", nodeId);
        json.addProperty("uuid", payload.uuid().toString());
        json.addProperty("stat", payload.statId());
        json.addProperty("source", payload.sourceId());
        json.addProperty("op", payload.operation());
        json.addProperty("value", payload.value());
        if (payload.expireAt() != null) {
            json.addProperty("expireAt", payload.expireAt());
        }
        redis.publishBroadcast(json);
    }

    public void broadcastModifierRemove(UUID uuid, String statId, String sourceId) {
        if (!active) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("type", "stat:modifier:remove");
        json.addProperty("node", nodeId);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("stat", statId);
        json.addProperty("source", sourceId);
        redis.publishBroadcast(json);
    }

    public void broadcastBase(UUID uuid, String statId, double value) {
        if (!active) {
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("type", "stat:base:set");
        json.addProperty("node", nodeId);
        json.addProperty("uuid", uuid.toString());
        json.addProperty("stat", statId);
        json.addProperty("value", value);
        redis.publishBroadcast(json);
    }

    @Override
    public void onMessage(String channel, JsonObject message) {
        if (!active) {
            return;
        }
        String origin = message.has("node") ? message.get("node").getAsString() : "";
        if (nodeId.equalsIgnoreCase(origin)) {
            return;
        }
        String type = message.get("type").getAsString();
        switch (type) {
            case "stat:modifier:add" -> handleModifierAdd(message);
            case "stat:modifier:remove" -> handleModifierRemove(message);
            case "stat:base:set" -> handleBase(message);
            default -> logger.fine("[TitlePlus] Unknown Redis payload: " + type);
        }
    }

    private void handleModifierAdd(JsonObject message) {
        try {
            UUID uuid = UUID.fromString(message.get("uuid").getAsString());
            String statId = message.get("stat").getAsString();
            String source = message.get("source").getAsString();
            String op = message.get("op").getAsString();
            double value = message.get("value").getAsDouble();
            Long expire = message.has("expireAt") && !message.get("expireAt").isJsonNull()
                    ? message.get("expireAt").getAsLong()
                    : null;
            statService.applyNetworkModifier(uuid, statId, source, op, value, expire);
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to apply network modifier", ex);
        }
    }

    private void handleModifierRemove(JsonObject message) {
        try {
            UUID uuid = UUID.fromString(message.get("uuid").getAsString());
            String statId = message.get("stat").getAsString();
            String source = message.get("source").getAsString();
            statService.applyNetworkModifierRemoval(uuid, statId, source);
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to remove network modifier", ex);
        }
    }

    private void handleBase(JsonObject message) {
        try {
            UUID uuid = UUID.fromString(message.get("uuid").getAsString());
            String statId = message.get("stat").getAsString();
            double value = message.get("value").getAsDouble();
            statService.applyNetworkBase(uuid, statId, value);
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to apply network base stat", ex);
        }
    }

    public record StatModifierPayload(UUID uuid, String statId, String sourceId, String operation, double value, Long expireAt) {
    }
}
