package wiki.creeper.creeperPrefixSystem.data.title;

import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffectType;
import wiki.creeper.creeperPrefixSystem.data.stat.StatModifier;

import java.util.Objects;

/**
 * Represents a single effect entry, typically loaded from YAML definitions. The plugin applies these
 * effects when a player equips a title or satisfies a set requirement.
 */
public final class TitleEffect {

    public enum Type {
        STAT_MOD,
        POTION,
        ATTRIBUTE,
        COMMAND
    }

    private final Type type;
    private final String statId;
    private final StatModifier.Operation statOperation;
    private final double value;
    private final PotionEffectType potionEffectType;
    private final int potionLevel;
    private final Attribute attribute;
    private final String command;

    private TitleEffect(Type type,
                        String statId,
                        StatModifier.Operation statOperation,
                        double value,
                        PotionEffectType potionEffectType,
                        int potionLevel,
                        Attribute attribute,
                        String command) {
        this.type = type;
        this.statId = statId;
        this.statOperation = statOperation;
        this.value = value;
        this.potionEffectType = potionEffectType;
        this.potionLevel = potionLevel;
        this.attribute = attribute;
        this.command = command;
    }

    public static TitleEffect stat(String statId, StatModifier.Operation operation, double value) {
        return new TitleEffect(Type.STAT_MOD, statId, operation, value, null, 0, null, null);
    }

    public static TitleEffect potion(PotionEffectType type, int level) {
        return new TitleEffect(Type.POTION, null, null, 0.0D, type, level, null, null);
    }

    public static TitleEffect attribute(Attribute attribute, double value) {
        return new TitleEffect(Type.ATTRIBUTE, null, null, value, null, 0, attribute, null);
    }

    public static TitleEffect command(String command) {
        return new TitleEffect(Type.COMMAND, null, null, 0.0D, null, 0, null, command);
    }

    public Type getType() {
        return type;
    }

    public String getStatId() {
        return statId;
    }

    public StatModifier.Operation getStatOperation() {
        return statOperation;
    }

    public double getValue() {
        return value;
    }

    public PotionEffectType getPotionEffectType() {
        return potionEffectType;
    }

    public int getPotionLevel() {
        return potionLevel;
    }

    public Attribute getAttribute() {
        return attribute;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TitleEffect that = (TitleEffect) o;
        return Double.compare(that.value, value) == 0 && potionLevel == that.potionLevel && type == that.type && Objects.equals(statId, that.statId) && statOperation == that.statOperation && Objects.equals(potionEffectType, that.potionEffectType) && attribute == that.attribute && Objects.equals(command, that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, statId, statOperation, value, potionEffectType, potionLevel, attribute, command);
    }
}
