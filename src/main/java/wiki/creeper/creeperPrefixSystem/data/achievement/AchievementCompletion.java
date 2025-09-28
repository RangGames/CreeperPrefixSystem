package wiki.creeper.creeperPrefixSystem.data.achievement;

import java.time.Instant;

/**
 * Represents an unlocked achievement for a player including completion order.
 */
public final class AchievementCompletion {

    private final String achievementId;
    private final Instant completedAt;
    private volatile long globalRank;

    public AchievementCompletion(String achievementId, Instant completedAt, long globalRank) {
        this.achievementId = achievementId;
        this.completedAt = completedAt;
        this.globalRank = globalRank;
    }

    public String getAchievementId() {
        return achievementId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public long getGlobalRank() {
        return globalRank;
    }

    public void setGlobalRank(long globalRank) {
        this.globalRank = globalRank;
    }
}

