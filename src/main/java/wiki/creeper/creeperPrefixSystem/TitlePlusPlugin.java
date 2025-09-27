package wiki.creeper.creeperPrefixSystem;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import wiki.creeper.creeperPrefixSystem.api.TitlePlusAPI;
import wiki.creeper.creeperPrefixSystem.api.TitlePlusAPIImpl;
import wiki.creeper.creeperPrefixSystem.command.RankCommand;
import wiki.creeper.creeperPrefixSystem.command.TitleAdminCommand;
import wiki.creeper.creeperPrefixSystem.command.TitleCommand;
import wiki.creeper.creeperPrefixSystem.command.TitlesCommand;
import wiki.creeper.creeperPrefixSystem.config.TitlePlusConfiguration;
import wiki.creeper.creeperPrefixSystem.data.set.SetRegistry;
import wiki.creeper.creeperPrefixSystem.data.stat.StatRegistry;
import wiki.creeper.creeperPrefixSystem.data.title.TitleRegistry;
import wiki.creeper.creeperPrefixSystem.listener.PlayerConnectionListener;
import wiki.creeper.creeperPrefixSystem.listener.GameplayListener;
import wiki.creeper.creeperPrefixSystem.redis.RedisBridge;
import wiki.creeper.creeperPrefixSystem.service.NetworkSyncService;
import wiki.creeper.creeperPrefixSystem.service.RequirementService;
import wiki.creeper.creeperPrefixSystem.service.SeasonService;
import wiki.creeper.creeperPrefixSystem.service.StatService;
import wiki.creeper.creeperPrefixSystem.service.TitleService;
import wiki.creeper.creeperPrefixSystem.service.WeeklyRankingService;
import wiki.creeper.creeperPrefixSystem.storage.MySqlStorage;
import wiki.creeper.creeperPrefixSystem.util.YamlLoader;

import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Root plugin entry point initialising all managers and coordinating lifecycle operations.
 */
public final class TitlePlusPlugin extends JavaPlugin {

    private TitlePlusConfiguration configuration;
    private ExecutorService executor;
    private MySqlStorage storage;
    private RedisBridge redis;
    private TitleRegistry titleRegistry;
    private SetRegistry setRegistry;
    private StatRegistry statRegistry;
    private StatService statService;
    private TitleService titleService;
    private SeasonService seasonService;
    private WeeklyRankingService weeklyRankingService;
    private TitlePlusAPIImpl api;
    private int seasonTaskId = -1;
    private int weeklyTaskId = -1;
    private RequirementService requirementService;
    private NetworkSyncService networkSyncService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Loading Paper configuration from " + getDataFolder().toPath().resolve("config.yml"));
        reloadConfiguration();

        executor = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()),
                runnable -> {
                    Thread thread = new Thread(runnable, "TitlePlus-Async");
                    thread.setDaemon(true);
                    return thread;
                });

        storage = new MySqlStorage(getLogger(), configuration.storage());
        try {
            storage.init();
        } catch (SQLException exception) {
            getLogger().severe("Unable to initialise database: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        redis = new RedisBridge(getLogger(), configuration.redis());
        redis.connect();

        titleRegistry = new TitleRegistry(this);
        setRegistry = new SetRegistry(this);
        statRegistry = new StatRegistry(this);

        FileConfiguration titlesConfig = YamlLoader.loadOrCopy(this, "titles.yml");
        FileConfiguration setsConfig = YamlLoader.loadOrCopy(this, "sets.yml");
        FileConfiguration statsConfig = YamlLoader.loadOrCopy(this, "stats.yml");
        titleRegistry.load(titlesConfig);
        setRegistry.load(setsConfig);
        statRegistry.load(statsConfig);

        statService = new StatService(this, statRegistry, storage, executor);
        titleService = new TitleService(this, titleRegistry, setRegistry, storage, statService, executor);
        seasonService = new SeasonService(getLogger(), storage, redis, executor, configuration);
        weeklyRankingService = new WeeklyRankingService(this, storage, redis, executor, configuration);
        requirementService = new RequirementService(titleRegistry, storage, titleService, weeklyRankingService, executor);
        networkSyncService = new NetworkSyncService(getLogger(), redis, statService, configuration.nodeId(), configuration.redis().enabled());
        statService.setNetworkSync(networkSyncService);

        seasonService.init();
        weeklyRankingService.refreshWeekKey();
        networkSyncService.init();

        api = new TitlePlusAPIImpl(statService, titleService, weeklyRankingService, seasonService, titleRegistry, setRegistry, statRegistry, requirementService);
        getServer().getServicesManager().register(TitlePlusAPI.class, api, this, ServicePriority.Normal);

        registerListeners();
        registerCommands();
        scheduleTasks();

        getLogger().info("TitlePlus enabled.");
    }

    @Override
    public void onDisable() {
        if (seasonTaskId != -1) {
            Bukkit.getScheduler().cancelTask(seasonTaskId);
        }
        if (weeklyTaskId != -1) {
            Bukkit.getScheduler().cancelTask(weeklyTaskId);
        }
        getServer().getServicesManager().unregister(api);
        if (networkSyncService != null) {
            networkSyncService.shutdown();
        }
        if (redis != null) {
            redis.close();
        }
        if (storage != null) {
            storage.close();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
        getLogger().info("TitlePlus disabled.");
    }

    public TitlePlusConfiguration getConfigurationModel() {
        return configuration;
    }

    public StatService getStatService() {
        return statService;
    }

    public TitleService getTitleService() {
        return titleService;
    }

    public WeeklyRankingService getWeeklyRankingService() {
        return weeklyRankingService;
    }

    public SeasonService getSeasonService() {
        return seasonService;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public TitleRegistry getTitleRegistry() {
        return titleRegistry;
    }

    public SetRegistry getSetRegistry() {
        return setRegistry;
    }

    public RequirementService getRequirementService() {
        return requirementService;
    }

    private void reloadConfiguration() {
        reloadConfig();
        configuration = TitlePlusConfiguration.from(getConfig());
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this, titleService, statService, requirementService), this);
        Bukkit.getPluginManager().registerEvents(new GameplayListener(this), this);
    }

    private void registerCommands() {
        Objects.requireNonNull(getCommand("titles"), "titles command missing in plugin.yml")
                .setExecutor(new TitlesCommand(this));

        PluginCommand titleCommand = Objects.requireNonNull(getCommand("title"), "title command missing");
        TitleCommand titleExecutor = new TitleCommand(this);
        titleCommand.setExecutor(titleExecutor);
        titleCommand.setTabCompleter(titleExecutor);

        PluginCommand adminCommand = Objects.requireNonNull(getCommand("titleadmin"), "titleadmin command missing");
        TitleAdminCommand adminExecutor = new TitleAdminCommand(this);
        adminCommand.setExecutor(adminExecutor);
        adminCommand.setTabCompleter(adminExecutor);

        PluginCommand rankCommand = Objects.requireNonNull(getCommand("rank"), "rank command missing");
        RankCommand rankExecutor = new RankCommand(this);
        rankCommand.setExecutor(rankExecutor);
        rankCommand.setTabCompleter(rankExecutor);
    }

    public void reloadAllResources() {
        reloadConfiguration();
        FileConfiguration titlesConfig = YamlLoader.loadOrCopy(this, "titles.yml");
        FileConfiguration setsConfig = YamlLoader.loadOrCopy(this, "sets.yml");
        FileConfiguration statsConfig = YamlLoader.loadOrCopy(this, "stats.yml");
        titleRegistry.load(titlesConfig);
        setRegistry.load(setsConfig);
        statRegistry.load(statsConfig);
        requirementService.rebuildIndexes();
        getLogger().info("TitlePlus configuration reloaded.");
    }

    private void scheduleTasks() {
        if (configuration.seasonAutoSync()) {
            long interval = Math.max(20L, configuration.seasonSyncIntervalTicks());
            seasonTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, seasonService::refreshFromDatabase, interval, interval).getTaskId();
        }
        long weeklyIntervalTicks = Math.max(20 * 60L, configuration.weeklyEvaluationIntervalMinutes() * 60L * 20L);
        weeklyTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> weeklyRankingService.evaluate(weeklyRankingService.getDefaultMetric()), weeklyIntervalTicks, weeklyIntervalTicks).getTaskId();
    }
}
