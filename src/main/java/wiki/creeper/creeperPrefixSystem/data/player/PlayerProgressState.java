package wiki.creeper.creeperPrefixSystem.data.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks per-title requirement progress for a player.
 */
public final class PlayerProgressState {

    private final Map<String, Long> progress = new HashMap<>();

    public Map<String, Long> asMap() {
        return Collections.unmodifiableMap(progress);
    }

    public long getProgress(String titleId) {
        return progress.getOrDefault(titleId, 0L);
    }

    public void setProgress(String titleId, long value) {
        progress.put(titleId, value);
    }

    public long addProgress(String titleId, long delta) {
        long value = getProgress(titleId) + delta;
        progress.put(titleId, value);
        return value;
    }
}
