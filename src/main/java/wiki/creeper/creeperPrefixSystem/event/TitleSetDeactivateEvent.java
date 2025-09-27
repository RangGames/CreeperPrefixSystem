package wiki.creeper.creeperPrefixSystem.event;

import org.bukkit.event.HandlerList;
import wiki.creeper.creeperPrefixSystem.data.set.SetDefinition;

import java.util.UUID;

/**
 * Fired after a set bonus has been removed from a player.
 */
public final class TitleSetDeactivateEvent extends AbstractSetEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public TitleSetDeactivateEvent(UUID playerId, SetDefinition definition, boolean async) {
        super(playerId, definition, async);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
