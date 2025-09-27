package wiki.creeper.creeperPrefixSystem.data.player;

import wiki.creeper.creeperPrefixSystem.data.stat.StatModifier;

import java.util.*;

/**
 * Holds the dynamic stat values and modifiers for a player in runtime memory.
 */
public final class PlayerStatState {
    private final Map<String, Double> baseValues = new HashMap<>();
    private final Map<String, Map<String, StatModifier>> modifiers = new HashMap<>();

    public double getBaseValue(String statId) {
        return baseValues.getOrDefault(statId, 0.0D);
    }

    public void setBaseValue(String statId, double value) {
        baseValues.put(statId, value);
    }

    public Collection<StatModifier> getModifiers(String statId) {
        return modifiers.getOrDefault(statId, Collections.emptyMap()).values();
    }

    public void putModifier(StatModifier modifier) {
        modifiers.computeIfAbsent(modifier.getStatId(), key -> new HashMap<>())
                .put(modifier.getSourceId(), modifier);
    }

    public boolean removeModifier(String statId, String sourceId) {
        Map<String, StatModifier> map = modifiers.get(statId);
        if (map == null) {
            return false;
        }
        boolean removed = map.remove(sourceId) != null;
        if (map.isEmpty()) {
            modifiers.remove(statId);
        }
        return removed;
    }

    public void clearExpiredModifiers(long currentEpoch) {
        Iterator<Map.Entry<String, Map<String, StatModifier>>> iterator = modifiers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Map<String, StatModifier>> entry = iterator.next();
            entry.getValue().values().removeIf(mod -> mod.getExpireAt() != null && mod.getExpireAt().toEpochMilli() < currentEpoch);
            if (entry.getValue().isEmpty()) {
                iterator.remove();
            }
        }
    }
}
