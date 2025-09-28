package wiki.creeper.creeperPrefixSystem.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import wiki.creeper.creeperPrefixSystem.service.AchievementService;
import wiki.creeper.creeperPrefixSystem.service.CollectionService;
import wiki.creeper.creeperPrefixSystem.service.RequirementService;
import wiki.creeper.creeperPrefixSystem.service.StatService;
import wiki.creeper.creeperPrefixSystem.service.TitleService;

import java.util.UUID;

/**
 * Handles loading and unloading of player data as they connect and disconnect from the server.
 */
public final class PlayerConnectionListener implements Listener {
    private final TitleService titleService;
    private final StatService statService;
    private final RequirementService requirementService;
    private final CollectionService collectionService;
    private final AchievementService achievementService;

    public PlayerConnectionListener(TitleService titleService,
                                    StatService statService,
                                    RequirementService requirementService,
                                    CollectionService collectionService,
                                    AchievementService achievementService) {
        this.titleService = titleService;
        this.statService = statService;
        this.requirementService = requirementService;
        this.collectionService = collectionService;
        this.achievementService = achievementService;
    }

    @EventHandler
    public void handleJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        statService.load(uuid);
        titleService.load(uuid);
        requirementService.load(uuid);
        collectionService.load(uuid);
        achievementService.load(uuid);
    }

    @EventHandler
    public void handleQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        statService.unload(uuid);
        titleService.unload(uuid);
        requirementService.unload(uuid);
        collectionService.unload(uuid);
        achievementService.unload(uuid);
    }
}
