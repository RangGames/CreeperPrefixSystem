package wiki.creeper.creeperPrefixSystem.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import wiki.creeper.creeperPrefixSystem.data.title.TitleDefinition;

import java.util.Optional;
import java.util.UUID;

/**
 * Base class for all title related events carrying the player identifier and resolved definition.
 */
public abstract class AbstractTitleEvent extends Event {

    private final UUID playerId;
    private final TitleDefinition definition;

    protected AbstractTitleEvent(UUID playerId, TitleDefinition definition, boolean async) {
        super(async);
        this.playerId = playerId;
        this.definition = definition;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public TitleDefinition getDefinition() {
        return definition;
    }

    public String getTitleId() {
        return definition.getId();
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(Bukkit.getPlayer(playerId));
    }
}
