package wiki.creeper.creeperPrefixSystem.data.achievement;

import java.util.Locale;

/**
 * Supported achievement trigger categories.
 */
public enum AchievementType {

    COLLECTION_COUNT;

    public static AchievementType fromString(String value, AchievementType fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return AchievementType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
