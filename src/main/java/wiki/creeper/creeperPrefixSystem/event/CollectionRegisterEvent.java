package wiki.creeper.creeperPrefixSystem.event;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Optional;
import java.util.UUID;

/**
 * Fired when a new item is about to be registered in a player's collection.
 * Plugins may cancel the registration or adjust XP rewards and ordering metadata.
 */
public final class CollectionRegisterEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final Material material;
    private final int previousCount;
    private int playerRank;
    private int xpReward;
    private boolean grantXp;
    private boolean announce = true;
    private boolean cancelled;

    public CollectionRegisterEvent(UUID playerId,
                                   Material material,
                                   int previousCount,
                                   int playerRank,
                                   int xpReward,
                                   boolean grantXp,
                                   boolean async) {
        super(async);
        this.playerId = playerId;
        this.material = material;
        this.previousCount = previousCount;
        this.playerRank = playerRank;
        this.xpReward = xpReward;
        this.grantXp = grantXp;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(Bukkit.getPlayer(playerId));
    }

    public Material getMaterial() {
        return material;
    }

    /**
     * @return number of entries before this registration.
     */
    public int getPreviousCount() {
        return previousCount;
    }

    /**
     * @return the rank/order the player will receive for this material.
     */
    public int getPlayerRank() {
        return playerRank;
    }

    public void setPlayerRank(int playerRank) {
        this.playerRank = Math.max(1, playerRank);
    }

    /**
     * @return XP reward to be granted if {@link #shouldGrantXp()} is true.
     */
    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = Math.max(0, xpReward);
    }

    public boolean shouldGrantXp() {
        return grantXp;
    }

    public void setGrantXp(boolean grantXp) {
        this.grantXp = grantXp;
    }

    public boolean shouldAnnounce() {
        return announce;
    }

    public void setAnnounce(boolean announce) {
        this.announce = announce;
    }

    public int getNewTotal() {
        return previousCount + 1;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
