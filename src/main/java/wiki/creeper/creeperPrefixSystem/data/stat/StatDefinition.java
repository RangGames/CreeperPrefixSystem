package wiki.creeper.creeperPrefixSystem.data.stat;

import java.util.Objects;

/**
 * Defines a stat that can be modified by titles, sets, or external plugins.
 */
public final class StatDefinition {

    private final String id;
    private final String displayName;
    private final double min;
    private final double max;
    private final double defaultValue;
    private final StatModifier.Operation stacking;

    public StatDefinition(String id, String displayName, double min, double max, double defaultValue, StatModifier.Operation stacking) {
        this.id = id;
        this.displayName = displayName;
        this.min = min;
        this.max = max;
        this.defaultValue = defaultValue;
        this.stacking = stacking;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public StatModifier.Operation getStacking() {
        return stacking;
    }

    public double clamp(double value) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatDefinition that = (StatDefinition) o;
        return Double.compare(that.min, min) == 0 && Double.compare(that.max, max) == 0 && Double.compare(that.defaultValue, defaultValue) == 0 && Objects.equals(id, that.id) && Objects.equals(displayName, that.displayName) && stacking == that.stacking;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, min, max, defaultValue, stacking);
    }
}
