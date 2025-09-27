package wiki.creeper.creeperPrefixSystem.data.title;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable definition of a title loaded from YAML or storage and cached in memory.
 */
public final class TitleDefinition {
    private final String id;
    private final String display;
    private final TitleRarity rarity;
    private final String type;
    private final TitleRequirement requirement;
    private final List<TitleEffect> effects;
    private final TitleSkin skin;
    private final boolean seasonal;
    private final boolean weeklyExclusive;

    public TitleDefinition(String id,
                           String display,
                           TitleRarity rarity,
                           String type,
                           TitleRequirement requirement,
                           List<TitleEffect> effects,
                           TitleSkin skin,
                           boolean seasonal,
                           boolean weeklyExclusive) {
        this.id = id;
        this.display = display;
        this.rarity = rarity;
        this.type = type;
        this.requirement = requirement;
        this.effects = List.copyOf(effects);
        this.skin = skin;
        this.seasonal = seasonal;
        this.weeklyExclusive = weeklyExclusive;
    }

    public String getId() {
        return id;
    }

    public String getDisplay() {
        return display;
    }

    public TitleRarity getRarity() {
        return rarity;
    }

    public String getType() {
        return type;
    }

    public TitleRequirement getRequirement() {
        return requirement;
    }

    public List<TitleEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    public TitleSkin getSkin() {
        return skin;
    }

    public boolean isSeasonal() {
        return seasonal;
    }

    public boolean isWeeklyExclusive() {
        return weeklyExclusive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TitleDefinition that = (TitleDefinition) o;
        return seasonal == that.seasonal && weeklyExclusive == that.weeklyExclusive && Objects.equals(id, that.id) && Objects.equals(display, that.display) && rarity == that.rarity && Objects.equals(type, that.type) && Objects.equals(requirement, that.requirement) && Objects.equals(effects, that.effects) && Objects.equals(skin, that.skin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, display, rarity, type, requirement, effects, skin, seasonal, weeklyExclusive);
    }
}
