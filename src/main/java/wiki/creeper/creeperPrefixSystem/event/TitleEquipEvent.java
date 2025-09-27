package wiki.creeper.creeperPrefixSystem.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import wiki.creeper.creeperPrefixSystem.data.title.TitleDefinition;

import java.util.UUID;

/**
 * Fired before a title is equipped. Cancelling the event keeps the previous state intact.
 */
public final class TitleEquipEvent extends AbstractTitleEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String previousTitleId;
    private boolean cancelled;

    public TitleEquipEvent(UUID playerId, TitleDefinition definition, String previousTitleId, boolean async) {
        super(playerId, definition, async);
        this.previousTitleId = previousTitleId;
    }

    public String getPreviousTitleId() {
        return previousTitleId;
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
