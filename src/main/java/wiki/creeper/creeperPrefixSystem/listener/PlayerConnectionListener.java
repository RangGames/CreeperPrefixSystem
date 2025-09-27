package wiki.creeper.creeperPrefixSystem.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import wiki.creeper.creeperPrefixSystem.TitlePlusPlugin;
import wiki.creeper.creeperPrefixSystem.service.RequirementService;
import wiki.creeper.creeperPrefixSystem.service.StatService;
import wiki.creeper.creeperPrefixSystem.service.TitleService;

import java.util.UUID;

/**
 * Handles loading and unloading of player data as they connect and disconnect from the server.
 */
public final class PlayerConnectionListener implements Listener {

    private final TitlePlusPlugin plugin;
    private final TitleService titleService;
    private final StatService statService;
    private final RequirementService requirementService;

    public PlayerConnectionListener(TitlePlusPlugin plugin, TitleService titleService, StatService statService, RequirementService requirementService) {
        this.plugin = plugin;
        this.titleService = titleService;
        this.statService = statService;
        this.requirementService = requirementService;
    }

    @EventHandler
    public void handleJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        statService.load(uuid);
        titleService.load(uuid);
        requirementService.load(uuid);
    }

    @EventHandler
    public void handleQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        statService.unload(uuid);
        titleService.unload(uuid);
        requirementService.unload(uuid);
    }
}
