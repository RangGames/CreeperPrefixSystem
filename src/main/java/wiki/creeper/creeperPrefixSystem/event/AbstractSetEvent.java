package wiki.creeper.creeperPrefixSystem.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import wiki.creeper.creeperPrefixSystem.data.set.SetDefinition;

import java.util.Optional;
import java.util.UUID;

/**
 * Base class for events concerning set activations and deactivations.
 */
public abstract class AbstractSetEvent extends Event {

    private final UUID playerId;
    private final SetDefinition definition;

    protected AbstractSetEvent(UUID playerId, SetDefinition definition, boolean async) {
        super(async);
        this.playerId = playerId;
        this.definition = definition;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public SetDefinition getDefinition() {
        return definition;
    }

    public String getSetId() {
        return definition.getId();
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(Bukkit.getPlayer(playerId));
    }
}
