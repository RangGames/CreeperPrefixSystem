package wiki.creeper.creeperPrefixSystem.data.ranking;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a cached weekly ranking entry.
 */
public final class WeeklyStanding implements Comparable<WeeklyStanding> {
    private final UUID playerId;
    private final String metric;
    private final long value;

    public WeeklyStanding(UUID playerId, String metric, long value) {
        this.playerId = playerId;
        this.metric = metric;
        this.value = value;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getMetric() {
        return metric;
    }

    public long getValue() {
        return value;
    }

    @Override
    public int compareTo(WeeklyStanding other) {
        return Long.compare(other.value, this.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeeklyStanding that = (WeeklyStanding) o;
        return value == that.value && Objects.equals(playerId, that.playerId) && Objects.equals(metric, that.metric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId, metric, value);
    }
}
