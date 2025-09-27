package wiki.creeper.creeperPrefixSystem.data.stat;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a modifier applied to a player's stat. Modifiers may expire and are uniquely identified by
 * the tuple {@code (player, statId, sourceId)} in persistence.
 */
public final class StatModifier {

    public enum Operation {
        ADD,
        MULT,
        SET
    }

    private final UUID playerId;
    private final String statId;
    private final String sourceId;
    private final Operation operation;
    private final double value;
    private final Instant expireAt;

    public StatModifier(UUID playerId, String statId, String sourceId, Operation operation, double value, Instant expireAt) {
        this.playerId = playerId;
        this.statId = statId;
        this.sourceId = sourceId;
        this.operation = operation;
        this.value = value;
        this.expireAt = expireAt;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getStatId() {
        return statId;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Operation getOperation() {
        return operation;
    }

    public double getValue() {
        return value;
    }

    public Instant getExpireAt() {
        return expireAt;
    }

    public boolean isExpired(Instant now) {
        return expireAt != null && expireAt.isBefore(now);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatModifier that = (StatModifier) o;
        return Double.compare(that.value, value) == 0 && Objects.equals(playerId, that.playerId) && Objects.equals(statId, that.statId) && Objects.equals(sourceId, that.sourceId) && operation == that.operation && Objects.equals(expireAt, that.expireAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, statId, sourceId, operation, value, expireAt);
    }
}
