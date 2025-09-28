package wiki.creeper.creeperPrefixSystem.listener;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import wiki.creeper.creeperPrefixSystem.service.CollectionService;

/**
 * 자연 드랍 아이템을 감지해 도감 등록 로직으로 위임한다.
 */
public final class CollectionListener implements Listener {

    private final CollectionService collectionService;

    public CollectionListener(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Item drop = event.getItemDrop();
        collectionService.markPlayerDrop(drop);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Item item = event.getItem();
        if (!collectionService.shouldConsider(item)) {
            return;
        }
        collectionService.handleNaturalPickup(player, item.getItemStack().getType());
    }
}
