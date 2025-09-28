package wiki.creeper.creeperPrefixSystem.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import wiki.creeper.creeperPrefixSystem.TitlePlusPlugin;
import wiki.creeper.creeperPrefixSystem.data.collection.CollectionEntry;
import wiki.creeper.creeperPrefixSystem.service.CollectionService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Displays the player's collection progress in chat.
 */
public final class CollectionCommand implements CommandExecutor {

    private final TitlePlusPlugin plugin;
    private final CollectionService collectionService;

    public CollectionCommand(TitlePlusPlugin plugin) {
        this.plugin = plugin;
        this.collectionService = plugin.getCollectionService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        List<CollectionEntry> entries = new ArrayList<>(collectionService.getEntries(uuid));
        if (entries.isEmpty()) {
            player.sendMessage("§7아직 등록된 도감이 없습니다. 자연적으로 아이템을 획득해보세요!");
            return true;
        }

        entries.sort(Comparator.comparingInt(CollectionEntry::getPlayerRank));

        player.sendMessage("§6[도감] §f보유 중: §e" + entries.size() + "개");

        int limit = Math.min(entries.size(), 10);
        for (int i = 0; i < limit; i++) {
            CollectionEntry entry = entries.get(i);
            Material material = entry.getMaterial();
            String displayName = collectionService.getDisplayName(material);
            StringBuilder line = new StringBuilder()
                    .append("§e").append(i + 1).append(". §f").append(displayName)
                    .append(" §7(#").append(entry.getPlayerRank()).append(")");
            if (entry.getGlobalRank() > 0) {
                line.append(" §8[전역 #").append(entry.getGlobalRank()).append("]");
            }
            player.sendMessage(line.toString());
        }

        if (entries.size() > limit) {
            player.sendMessage("§7... 총 " + entries.size() + "개의 도감을 보유 중입니다.");
        }

        return true;
    }
}

