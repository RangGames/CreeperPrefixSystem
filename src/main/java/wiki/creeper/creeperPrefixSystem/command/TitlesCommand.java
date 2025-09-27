package wiki.creeper.creeperPrefixSystem.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import wiki.creeper.creeperPrefixSystem.TitlePlusPlugin;
import wiki.creeper.creeperPrefixSystem.gui.TitleCollectionGui;

/**
 * /titles GUI command entry point.
 */
public final class TitlesCommand implements CommandExecutor {

    private final TitlePlusPlugin plugin;

    public TitlesCommand(TitlePlusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!player.hasPermission("titles.gui")) {
            player.sendMessage("§c해당 GUI를 열 권한이 없습니다.");
            return true;
        }
        new TitleCollectionGui(plugin, player).open();
        return true;
    }
}
