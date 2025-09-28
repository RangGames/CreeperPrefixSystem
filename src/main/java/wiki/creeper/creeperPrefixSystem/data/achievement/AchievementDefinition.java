package wiki.creeper.creeperPrefixSystem.data.achievement;

/**
 * Describes a single achievement and its unlocking criteria.
 */
public final class AchievementDefinition {

    private final String id;
    private final String display;
    private final String description;
    private final AchievementType type;
    private final int target;

    public AchievementDefinition(String id, String display, String description, AchievementType type, int target) {
        this.id = id;
        this.display = display;
        this.description = description;
        this.type = type;
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public String getDisplay() {
        return display;
    }

    public String getDescription() {
        return description;
    }

    public AchievementType getType() {
        return type;
    }

    public int getTarget() {
        return target;
    }

    public boolean matchesCollectionCount(int count) {
        return type == AchievementType.COLLECTION_COUNT && count >= target;
    }
}
