package wiki.creeper.creeperPrefixSystem.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wiki.creeper.creeperPrefixSystem.TitlePlusPlugin;
import wiki.creeper.creeperPrefixSystem.data.title.TitleDefinition;
import wiki.creeper.creeperPrefixSystem.service.TitleService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Command handler for /title operations.
 */
public final class TitleCommand implements CommandExecutor, TabCompleter {

    private final TitlePlusPlugin plugin;
    private final TitleService titleService;

    public TitleCommand(TitlePlusPlugin plugin) {
        this.plugin = plugin;
        this.titleService = plugin.getTitleService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e/title <equip|unequip|info> ...");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        UUID uuid = player.getUniqueId();
        switch (sub) {
            case "equip" -> {
                if (args.length < 2) {
                    player.sendMessage("§c/title equip <titleId>");
                    return true;
                }
                String titleId = args[1];
                if (titleService.equipTitle(uuid, titleId)) {
                    TitleDefinition def = plugin.getTitleRegistry().get(titleId);
                    player.sendMessage("§a칭호 장착 완료: " + (def != null ? def.getDisplay() : titleId));
                } else {
                    player.sendMessage("§c칭호를 장착할 수 없습니다. 보유 여부를 확인하세요.");
                }
            }
            case "unequip" -> {
                if (titleService.unequip(uuid)) {
                    player.sendMessage("§e칭호를 해제했습니다.");
                } else {
                    player.sendMessage("§c현재 장착된 칭호가 없습니다.");
                }
            }
            case "info" -> {
                String titleId;
                if (args.length >= 2) {
                    titleId = args[1];
                } else {
                    titleId = titleService.getEquippedTitle(uuid).orElse(null);
                }
                if (titleId == null) {
                    player.sendMessage("§c확인할 칭호를 선택하세요.");
                    return true;
                }
                TitleDefinition def = plugin.getTitleRegistry().get(titleId);
                if (def == null) {
                    player.sendMessage("§c존재하지 않는 칭호입니다.");
                    return true;
                }
                player.sendMessage("§6칭호 정보: " + def.getDisplay());
                player.sendMessage("§f희귀도: " + def.getRarity().name());
                player.sendMessage("§f타입: " + def.getType());
                def.getEffects().forEach(effect -> player.sendMessage("§7- " + effect.getType().name()));
            }
            default -> player.sendMessage("§c알 수 없는 하위 명령입니다.");
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("equip", "unequip", "info");
        }
        if (args.length == 2 && sender instanceof Player player) {
            List<String> results = new ArrayList<>();
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "equip", "info" -> {
                    titleService.getOwnedTitles(player.getUniqueId()).forEach(titleId -> {
                        if (titleId.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                            results.add(titleId);
                        }
                    });
                    if (results.isEmpty()) {
                        plugin.getTitleRegistry().all().forEach(def -> {
                            if (def.getId().startsWith(args[1].toLowerCase(Locale.ROOT))) {
                                results.add(def.getId());
                            }
                        });
                    }
                }
            }
            return results;
        }
        return List.of();
    }
}
