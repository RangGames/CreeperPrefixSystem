package wiki.creeper.creeperPrefixSystem;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import wiki.creeper.creeperPrefixSystem.config.RedisConfig;
import wiki.creeper.creeperPrefixSystem.redis.RedisBridge;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;
import wiki.creeper.creeperPrefixSystem.config.ProxyConfig;
import wiki.creeper.creeperPrefixSystem.config.ProxyConfigurationLoader;
import wiki.creeper.creeperPrefixSystem.service.PlayerNameCache;
import wiki.creeper.creeperPrefixSystem.service.ProxySeasonCoordinator;
import wiki.creeper.creeperPrefixSystem.service.ProxyWeeklyCoordinator;
import wiki.creeper.creeperPrefixSystem.data.season.SeasonState;
import wiki.creeper.creeperPrefixSystem.data.ranking.WeeklyStanding;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

@Plugin(id = "titleplus", name = "TitlePlusProxy", version = "1.0-SNAPSHOT", authors = {"Creeper"})
public final class TitlePlusVelocityPlugin {

    private final ProxyServer proxy;
    private final Logger logger;
    private final CommandManager commandManager;
    private final Path dataDirectory;

    private final ExecutorService executor = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r, "TitlePlus-ProxyAsync");
        thread.setDaemon(true);
        return thread;
    });

    private ProxyConfig config;
    private MySqlStorage storage;
    private RedisBridge redis;
    private ProxySeasonCoordinator seasonCoordinator;
    private ProxyWeeklyCoordinator weeklyCoordinator;
    private PlayerNameCache nameCache;
    private final AtomicReference<com.velocitypowered.api.scheduler.ScheduledTask> seasonTask = new AtomicReference<>();
    private final AtomicReference<com.velocitypowered.api.scheduler.ScheduledTask> weeklyTask = new AtomicReference<>();

    @Inject
    public TitlePlusVelocityPlugin(ProxyServer proxy,
                                   Logger logger,
                                   CommandManager commandManager,
                                   @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.commandManager = commandManager;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void handleInit(ProxyInitializeEvent event) {
        ProxyConfigurationLoader loader = new ProxyConfigurationLoader(logger, dataDirectory);
        config = loader.load();
        logger.info("TitlePlus proxy configuration loaded from " + dataDirectory.resolve("titleplus-proxy.json"));

        storage = new MySqlStorage(logger, config.mysql());
        try {
            storage.init();
        } catch (SQLException ex) {
            logger.severe("Failed to initialise TitlePlus storage: " + ex.getMessage());
            return;
        }

        RedisConfig redisConfig = config.redis();
        if (redisConfig.enabled()) {
            redis = new RedisBridge(logger, redisConfig);
            redis.connect();
        } else {
            redis = null;
            logger.info("[TitlePlus-Proxy] Redis disabled in configuration");
        }

        seasonCoordinator = new ProxySeasonCoordinator(logger, storage, redis, executor);
        weeklyCoordinator = new ProxyWeeklyCoordinator(logger, storage, redis, executor, config.defaultWeeklyMetric());
        nameCache = new PlayerNameCache(proxy, logger);

        seasonCoordinator.init();
        weeklyCoordinator.refreshWeekKey();

        registerCommands();
        scheduleTasks();
        logger.info("TitlePlus proxy component initialised.");
    }

    @Subscribe
    public void handleShutdown(ProxyShutdownEvent event) {
        cancelTasks();
        if (redis != null) {
            redis.close();
        }
        if (storage != null) {
            storage.close();
        }
        executor.shutdownNow();
    }

    private void registerCommands() {
        commandManager.register(commandManager.metaBuilder("season").build(), new SeasonCommand());
        commandManager.register(commandManager.metaBuilder("rank").aliases("proxtop").build(), new RankCommand());
    }

    private void scheduleTasks() {
        if (config.seasonAutoSync()) {
            Duration period = Duration.ofSeconds(Math.max(60L, config.seasonSyncIntervalSeconds()));
            seasonTask.set(proxy.getScheduler().buildTask(this, seasonCoordinator::refresh)
                    .delay(period)
                    .repeat(period)
                    .schedule());
        }
        Duration weeklyPeriod = Duration.ofMinutes(Math.max(1L, config.weeklyEvaluationMinutes()));
        weeklyTask.set(proxy.getScheduler().buildTask(this, () -> {
            weeklyCoordinator.refreshWeekKey();
            weeklyCoordinator.evaluate(weeklyCoordinator.getDefaultMetric())
                    .exceptionally(ex -> {
                        logger.severe("Failed to evaluate weekly rankings: " + ex.getMessage());
                        return null;
                    });
        }).delay(weeklyPeriod)
                .repeat(weeklyPeriod)
                .schedule());
    }

    private void cancelTasks() {
        com.velocitypowered.api.scheduler.ScheduledTask season = seasonTask.getAndSet(null);
        if (season != null) {
            season.cancel();
        }
        com.velocitypowered.api.scheduler.ScheduledTask weekly = weeklyTask.getAndSet(null);
        if (weekly != null) {
            weekly.cancel();
        }
    }

    private final class SeasonCommand implements SimpleCommand {

        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length == 0) {
                invocation.source().sendMessage(Component.text("Usage: /season <state|set>").color(NamedTextColor.YELLOW));
                return;
            }
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "state":
                    SeasonState state = seasonCoordinator.getState();
                    invocation.source().sendMessage(Component.text("Current season state: " + state));
                    return;
                case "set":
                    if (args.length < 2) {
                        invocation.source().sendMessage(Component.text("Usage: /season set <state>").color(NamedTextColor.YELLOW));
                        return;
                    }
                    if (!invocation.source().hasPermission("titleplus.admin")) {
                        invocation.source().sendMessage(Component.text("You lack permission.").color(NamedTextColor.RED));
                        return;
                    }
                    try {
                        SeasonState newState = SeasonState.valueOf(args[1].toUpperCase(Locale.ROOT));
                        seasonCoordinator.updateState(newState);
                        invocation.source().sendMessage(Component.text("Season state updated to " + newState).color(NamedTextColor.GREEN));
                    } catch (IllegalArgumentException ex) {
                        invocation.source().sendMessage(Component.text("Unknown state.").color(NamedTextColor.RED));
                    }
                    return;
                default:
                    invocation.source().sendMessage(Component.text("Unknown subcommand.").color(NamedTextColor.RED));
            }
        }

        @Override
        public List<String> suggest(SimpleCommand.Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length == 0) {
                return List.of("state", "set");
            }
            if (args.length == 1) {
                return Arrays.stream(new String[]{"state", "set"})
                        .filter(opt -> opt.startsWith(args[0].toLowerCase(Locale.ROOT)))
                        .toList();
            }
            if (args.length == 2 && "set".equalsIgnoreCase(args[0])) {
                return Arrays.stream(SeasonState.values())
                        .map(state -> state.name().toLowerCase(Locale.ROOT))
                        .filter(name -> name.startsWith(args[1].toLowerCase(Locale.ROOT)))
                        .toList();
            }
            return List.of();
        }
    }

    private final class RankCommand implements SimpleCommand {

        @Override
        public void execute(SimpleCommand.Invocation invocation) {
            String[] args = invocation.arguments();
            String metric = args.length >= 1 ? args[0].toUpperCase(Locale.ROOT) : weeklyCoordinator.getDefaultMetric();
            var source = invocation.source();
            weeklyCoordinator.evaluate(metric)
                    .thenCompose(standings -> {
                        List<CompletableFuture<String>> names = new ArrayList<>();
                        for (WeeklyStanding standing : standings) {
                            names.add(nameCache.lookup(standing.getPlayerId()));
                        }
                        CompletableFuture<Void> joined = CompletableFuture.allOf(names.toArray(new CompletableFuture[0]));
                        return joined.thenApply(v -> new Result(standings, names.stream().map(CompletableFuture::join).toList()));
                    })
                    .thenAccept(result -> proxy.getScheduler().buildTask(TitlePlusVelocityPlugin.this, () -> {
                        source.sendMessage(Component.text("Weekly standings for " + metric + ":"));
                        if (result.standings().isEmpty()) {
                            source.sendMessage(Component.text("No data available."));
                            return;
                        }
                        for (int i = 0; i < result.standings().size(); i++) {
                            WeeklyStanding standing = result.standings().get(i);
                            String name = result.names().get(i);
                            source.sendMessage(Component.text((i + 1) + ". " + name + " - " + standing.getValue()));
                        }
                    }).schedule())
                    .exceptionally(ex -> {
                        logger.severe("Failed to evaluate weekly rankings: " + ex.getMessage());
                        return null;
                    });
        }

        @Override
        public List<String> suggest(SimpleCommand.Invocation invocation) {
            return List.of(weeklyCoordinator.getDefaultMetric());
        }
    }

    private record Result(List<WeeklyStanding> standings, List<String> names) {
    }
}
