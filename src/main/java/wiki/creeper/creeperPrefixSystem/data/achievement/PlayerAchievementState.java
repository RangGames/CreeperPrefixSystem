package wiki.creeper.creeperPrefixSystem.data.achievement;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks completed achievements for a player.
 */
public final class PlayerAchievementState {

    private final Map<String, AchievementCompletion> completions = new ConcurrentHashMap<>();

    public void addCompletion(AchievementCompletion completion) {
        completions.put(completion.getAchievementId(), completion);
    }

    public boolean hasCompletion(String achievementId) {
        return completions.containsKey(achievementId);
    }

    public AchievementCompletion getCompletion(String achievementId) {
        return completions.get(achievementId);
    }

    public Collection<AchievementCompletion> getCompletions() {
        return Collections.unmodifiableCollection(completions.values());
    }
}

