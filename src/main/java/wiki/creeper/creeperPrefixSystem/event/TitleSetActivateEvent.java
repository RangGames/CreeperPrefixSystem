package wiki.creeper.creeperPrefixSystem.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import wiki.creeper.creeperPrefixSystem.data.set.SetDefinition;

import java.util.UUID;

/**
 * Fired before a set bonus is activated for a player.
 */
public final class TitleSetActivateEvent extends AbstractSetEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    public TitleSetActivateEvent(UUID playerId, SetDefinition definition, boolean async) {
        super(playerId, definition, async);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
