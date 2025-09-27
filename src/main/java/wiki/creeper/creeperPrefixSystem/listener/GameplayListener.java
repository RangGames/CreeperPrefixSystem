package wiki.creeper.creeperPrefixSystem.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import wiki.creeper.creeperPrefixSystem.TitlePlusPlugin;
import wiki.creeper.creeperPrefixSystem.service.RequirementService;

/**
 * Captures gameplay interactions to advance title requirements and weekly metrics.
 */
public final class GameplayListener implements Listener {

    private final RequirementService requirementService;

    public GameplayListener(TitlePlusPlugin plugin) {
        this.requirementService = plugin.getRequirementService();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        requirementService.handleBlockBreak(event.getPlayer(), event.getBlock().getType());
    }
}
