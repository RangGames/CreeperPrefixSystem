package wiki.creeper.creeperPrefixSystem.data.set;

import wiki.creeper.creeperPrefixSystem.data.title.TitleEffect;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a group of titles that provide additional effects when the player satisfies the required
 * membership rules.
 */
public final class SetDefinition {
    private final String id;
    private final String displayName;
    private final List<String> members;
    private final List<TitleEffect> effects;

    public SetDefinition(String id, String displayName, List<String> members, List<TitleEffect> effects) {
        this.id = id;
        this.displayName = displayName;
        this.members = List.copyOf(members);
        this.effects = List.copyOf(effects);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<TitleEffect> getEffects() {
        return Collections.unmodifiableList(effects);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SetDefinition that = (SetDefinition) o;
        return Objects.equals(id, that.id) && Objects.equals(displayName, that.displayName) && Objects.equals(members, that.members) && Objects.equals(effects, that.effects);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, members, effects);
    }
}
