package wiki.creeper.creeperPrefixSystem.config;

/**
 * Immutable configuration for SQL storage.
 */
public record StorageConfig(String host,
                            int port,
                            String database,
                            String username,
                            String password,
                            int maximumPoolSize,
                            int minimumIdle,
                            long connectionTimeout) {
}
