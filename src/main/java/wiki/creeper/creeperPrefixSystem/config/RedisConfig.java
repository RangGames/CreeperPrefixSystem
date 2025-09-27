package wiki.creeper.creeperPrefixSystem.config;

/**
 * Settings required for establishing Redis connectivity.
 */
public record RedisConfig(boolean enabled,
                          String host,
                          int port,
                          String username,
                          String password,
                          String broadcastChannel,
                          String apiRequestChannel,
                          String apiReplyChannel,
                          String seasonKey,
                          String weekKey,
                          String weeklyTopPrefix) {
}
