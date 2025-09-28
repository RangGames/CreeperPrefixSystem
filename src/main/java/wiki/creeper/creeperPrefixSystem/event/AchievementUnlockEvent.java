package wiki.creeper.creeperPrefixSystem.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementDefinition;

import java.util.Optional;
import java.util.UUID;

/**
 * Fired when a player is about to receive an achievement. Plugins can cancel or suppress announcements.
 */
public final class AchievementUnlockEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final AchievementDefinition definition;
    private final int completionOrder;
    private boolean announce = true;
    private boolean cancelled;

    public AchievementUnlockEvent(UUID playerId, AchievementDefinition definition, int completionOrder, boolean async) {
        super(async);
        this.playerId = playerId;
        this.definition = definition;
        this.completionOrder = completionOrder;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(Bukkit.getPlayer(playerId));
    }

    public AchievementDefinition getDefinition() {
        return definition;
    }

    public String getAchievementId() {
        return definition.getId();
    }

    /**
     * @return number of achievements the player will have after this unlock.
     */
    public int getCompletionOrder() {
        return completionOrder;
    }

    public boolean shouldAnnounce() {
        return announce;
    }

    public void setAnnounce(boolean announce) {
        this.announce = announce;
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

