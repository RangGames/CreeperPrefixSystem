package wiki.creeper.creeperPrefixSystem.data.title;

import org.bukkit.Material;

import java.util.Objects;

/**
 * Requirement that needs to be satisfied before a title becomes available to a player.
 */
public final class TitleRequirement {

    public enum Type {
        SELL,
        BREAK,
        EVENT,
        COMMAND
    }

    private final Type type;
    private final Material material;
    private final long amount;
    private final String meta;

    private TitleRequirement(Type type, Material material, long amount, String meta) {
        this.type = type;
        this.material = material;
        this.amount = amount;
        this.meta = meta;
    }

    public static TitleRequirement none() {
        return new TitleRequirement(Type.EVENT, null, 0, null);
    }

    public static TitleRequirement of(Type type, Material material, long amount, String meta) {
        return new TitleRequirement(type, material, amount, meta);
    }

    public Type getType() {
        return type;
    }

    public Material getMaterial() {
        return material;
    }

    public long getAmount() {
        return amount;
    }

    public String getMeta() {
        return meta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TitleRequirement that = (TitleRequirement) o;
        return amount == that.amount && type == that.type && material == that.material && Objects.equals(meta, that.meta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, material, amount, meta);
    }
}
