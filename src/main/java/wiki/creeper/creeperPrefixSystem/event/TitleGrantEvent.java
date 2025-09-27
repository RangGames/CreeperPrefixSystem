package wiki.creeper.creeperPrefixSystem.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import wiki.creeper.creeperPrefixSystem.data.title.TitleDefinition;

import java.util.UUID;

/**
 * Fired before a title is granted to a player. Cancelling the event prevents the grant.
 */
public final class TitleGrantEvent extends AbstractTitleEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    public TitleGrantEvent(UUID playerId, TitleDefinition definition, boolean async) {
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
