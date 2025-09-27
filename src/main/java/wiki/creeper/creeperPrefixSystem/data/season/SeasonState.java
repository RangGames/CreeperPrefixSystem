package wiki.creeper.creeperPrefixSystem.data.season;

/**
 * Represents the lifecycle state of a season that can be synchronized between proxy and spigot nodes.
 */
public enum SeasonState {
    PREPARING,
    RUNNING,
    PAUSED,
    ENDED
}
