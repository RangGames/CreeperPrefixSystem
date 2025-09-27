package wiki.creeper.creeperPrefixSystem.command;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wiki.creeper.creeperPrefixSystem.TitlePlusPlugin;
import wiki.creeper.creeperPrefixSystem.data.season.SeasonState;
import wiki.creeper.creeperPrefixSystem.data.title.TitleDefinition;

import java.util.*;

/**
 * Administrative command handler for TitlePlus allowing manual management and diagnostics.
 */
public final class TitleAdminCommand implements CommandExecutor, TabCompleter {

    private final TitlePlusPlugin plugin;

    public TitleAdminCommand(TitlePlusPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        if (!sender.hasPermission("titles.admin")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§e/titleadmin <grant|revoke|reload|season|weekly|progress|sell>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "grant" -> handleGrant(sender, args);
            case "revoke" -> handleRevoke(sender, args);
            case "reload" -> handleReload(sender);
            case "season" -> handleSeason(sender, args);
            case "weekly" -> handleWeekly(sender, args);
            case "progress" -> handleProgress(sender, args);
            case "sell" -> handleSell(sender, args);
            default -> sender.sendMessage("§c알 수 없는 하위 명령입니다.");
        }
        return true;
    }

    private void handleGrant(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c/titleadmin grant <player> <titleId>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId() == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다.");
            return;
        }
        String titleId = args[2];
        boolean success = plugin.getTitleService().grantTitle(target.getUniqueId(), titleId);
        if (success) {
            sender.sendMessage("§a" + target.getName() + "에게 칭호를 지급했습니다.");
        } else {
            sender.sendMessage("§c칭호 지급에 실패했습니다. 이미 보유 중일 수 있습니다.");
        }
    }

    private void handleRevoke(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c/titleadmin revoke <player> <titleId>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId() == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다.");
            return;
        }
        String titleId = args[2];
        boolean success = plugin.getTitleService().revokeTitle(target.getUniqueId(), titleId);
        if (success) {
            sender.sendMessage("§e" + target.getName() + "의 칭호를 회수했습니다.");
        } else {
            sender.sendMessage("§c해당 칭호를 보유하고 있지 않습니다.");
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadAllResources();
        sender.sendMessage("§aTitlePlus 설정과 데이터가 리로드되었습니다.");
    }

    private void handleSeason(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c/titleadmin season <PREPARING|RUNNING|PAUSED|ENDED>");
            return;
        }
        try {
            SeasonState state = SeasonState.valueOf(args[1].toUpperCase(Locale.ROOT));
            plugin.getSeasonService().setState(state);
            sender.sendMessage("§a시즌 상태를 " + state + " 로 변경했습니다.");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§c잘못된 시즌 상태입니다.");
        }
    }

    private void handleWeekly(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c/titleadmin weekly evaluate [metric]");
            return;
        }
        if (!"evaluate".equalsIgnoreCase(args[1])) {
            sender.sendMessage("§c지원하지 않는 하위 명령입니다.");
            return;
        }
        String metric = args.length >= 3 ? args[2] : plugin.getWeeklyRankingService().getDefaultMetric();
        plugin.getWeeklyRankingService().evaluate(metric)
                .thenRun(() -> plugin.getServer().getScheduler().runTask(plugin, () -> sender.sendMessage("§a주간 랭킹을 갱신했습니다.")));
    }

    private void handleProgress(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c/titleadmin progress <player> <titleId> <amount>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId() == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다.");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§c숫자를 입력하세요.");
            return;
        }
        plugin.getRequirementService().addProgress(target.getUniqueId(), args[2], amount);
        sender.sendMessage("§a진행도가 업데이트되었습니다.");
    }

    private void handleSell(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c/titleadmin sell <player> <material> <amount>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId() == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다.");
            return;
        }
        Material material = Material.matchMaterial(args[2].toUpperCase(Locale.ROOT));
        if (material == null) {
            sender.sendMessage("§c알 수 없는 재료입니다.");
            return;
        }
        long amount;
        try {
            amount = Long.parseLong(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage("§c숫자를 입력하세요.");
            return;
        }
        plugin.getRequirementService().handleSell(target.getUniqueId(), material, amount);
        sender.sendMessage("§a판매 기록이 반영되었습니다.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String alias,
                                                @NotNull String[] args) {
        if (!sender.hasPermission("titles.admin")) {
            return List.of();
        }
        if (args.length == 1) {
            return Arrays.asList("grant", "revoke", "reload", "season", "weekly", "progress", "sell");
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "grant", "revoke" -> {
                    return null; // Bukkit will suggest online players.
                }
                case "progress", "sell" -> {
                    return null;
                }
                case "season" -> {
                    List<String> states = new ArrayList<>();
                    for (SeasonState state : SeasonState.values()) {
                        states.add(state.name().toLowerCase(Locale.ROOT));
                    }
                    return states;
                }
                case "weekly" -> {
                    return List.of("evaluate");
                }
            }
        }
        if (args.length == 3) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "grant", "revoke" -> {
                    List<String> ids = new ArrayList<>();
                    for (TitleDefinition definition : plugin.getTitleRegistry().all()) {
                        if (definition.getId().startsWith(args[2].toLowerCase(Locale.ROOT))) {
                            ids.add(definition.getId());
                        }
                    }
                    return ids;
                }
                case "progress" -> {
                    List<String> ids = new ArrayList<>();
                    for (TitleDefinition definition : plugin.getTitleRegistry().all()) {
                        if (definition.getId().startsWith(args[2].toLowerCase(Locale.ROOT))) {
                            ids.add(definition.getId());
                        }
                    }
                    return ids;
                }
                case "sell" -> {
                    List<String> matches = new ArrayList<>();
                    for (Material material : Material.values()) {
                        if (!material.isItem()) {
                            continue;
                        }
                        String name = material.name().toLowerCase(Locale.ROOT);
                        if (name.startsWith(args[2].toLowerCase(Locale.ROOT))) {
                            matches.add(name);
                            if (matches.size() >= 50) {
                                break;
                            }
                        }
                    }
                    return matches;
                }
                case "weekly" -> {
                    Set<String> metrics = new HashSet<>(plugin.getWeeklyRankingService().getCachedMetrics());
                    if (metrics.isEmpty()) {
                        metrics.add(plugin.getWeeklyRankingService().getDefaultMetric());
                    }
                    return new ArrayList<>(metrics);
                }
            }
        }
        return List.of();
    }
}
