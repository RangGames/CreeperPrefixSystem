package wiki.creeper.creeperPrefixSystem.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wiki.creeper.creeperPrefixSystem.TitlePlusPlugin;
import wiki.creeper.creeperPrefixSystem.data.ranking.WeeklyStanding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Provides runtime access to weekly rankings for players.
 */
public final class RankCommand implements CommandExecutor, TabCompleter {

    private final TitlePlusPlugin plugin;

    public RankCommand(TitlePlusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }
        if (!sender.hasPermission("titles.use")) {
            sender.sendMessage("§c권한이 없습니다.");
            return true;
        }
        String metric = args.length >= 1 ? args[0] : plugin.getWeeklyRankingService().getDefaultMetric();
        CompletableFuture<List<WeeklyStanding>> future = plugin.getWeeklyRankingService().evaluate(metric);
        future.thenAccept(standings -> plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.sendMessage("§6주간 랭킹 - " + metric.toUpperCase(Locale.ROOT));
            if (standings.isEmpty()) {
                player.sendMessage("§7아직 데이터가 없습니다.");
                return;
            }
            int position = 1;
            for (WeeklyStanding standing : standings) {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(standing.getPlayerId());
                player.sendMessage("§e" + position + ". §f" + offline.getName() + " §7- §b" + standing.getValue());
                position++;
            }
        })).exceptionally(ex -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> player.sendMessage("§c랭킹 정보를 불러오지 못했습니다."));
            plugin.getLogger().severe("Failed to evaluate rankings: " + ex.getMessage());
            return null;
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (args.length == 1) {
            List<String> metrics = new ArrayList<>(plugin.getWeeklyRankingService().getCachedMetrics());
            if (metrics.isEmpty()) {
                metrics.add(plugin.getWeeklyRankingService().getDefaultMetric());
            }
            return metrics;
        }
        return List.of();
    }
}
