package wiki.creeper.creeperPrefixSystem.data.collection;

import org.bukkit.Material;

import java.time.Instant;

/**
 * Represents a single collection entry a player has unlocked.
 */
public final class CollectionEntry {

    private final Material material;
    private final Instant registeredAt;
    private final int playerRank;
    private volatile long globalRank;

    public CollectionEntry(Material material, Instant registeredAt, int playerRank, long globalRank) {
        this.material = material;
        this.registeredAt = registeredAt;
        this.playerRank = playerRank;
        this.globalRank = globalRank;
    }

    public Material getMaterial() {
        return material;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public int getPlayerRank() {
        return playerRank;
    }

    public long getGlobalRank() {
        return globalRank;
    }

    public void setGlobalRank(long globalRank) {
        this.globalRank = globalRank;
    }
}
