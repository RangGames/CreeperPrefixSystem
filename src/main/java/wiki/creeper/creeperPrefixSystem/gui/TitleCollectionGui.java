package wiki.creeper.creeperPrefixSystem.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import wiki.creeper.creeperPrefixSystem.TitlePlusPlugin;
import wiki.creeper.creeperPrefixSystem.data.title.TitleDefinition;
import wiki.creeper.creeperPrefixSystem.data.title.TitleEffect;
import wiki.creeper.creeperPrefixSystem.data.title.TitleRarity;
import wiki.creeper.creeperPrefixSystem.data.title.TitleRequirement;
import wiki.creeper.creeperPrefixSystem.service.RequirementService;
import wiki.creeper.creeperPrefixSystem.service.TitleService;
import wiki.creeper.creeperPrefixSystem.util.TextUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Simple GUI that allows players to browse unlocked titles and equip them quickly.
 */
public final class TitleCollectionGui implements Listener {

    private final TitlePlusPlugin plugin;
    private final TitleService titleService;
    private final RequirementService requirementService;
    private final Player player;

    private Inventory inventory;
    private TitleRarity rarityFilter;
    private String typeFilter;
    private int nameFilterIndex;
    private boolean ownedOnly;
    private final List<TitleDefinition> currentView = new ArrayList<>();

    private static final TitleRarity[] RARITY_CYCLE = TitleRarity.values();
    private static final String[] NAME_FILTERS = {"전체", "A-F", "G-L", "M-R", "S-Z", "기타"};

    public TitleCollectionGui(TitlePlusPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.titleService = plugin.getTitleService();
        this.requirementService = plugin.getRequirementService();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open() {
        inventory = Bukkit.createInventory(player, 54, "§6칭호 도감");
        refresh();
        player.openInventory(inventory);
    }

    private void refresh() {
        inventory.clear();
        renderFilters();
        this.currentView.clear();
        List<TitleDefinition> titles = plugin.getTitleRegistry().all().stream()
                .sorted(Comparator.comparing(TitleDefinition::getRarity)
                        .thenComparing(TitleDefinition::getDisplay, String.CASE_INSENSITIVE_ORDER))
                .filter(this::passesFilters)
                .collect(Collectors.toList());

        UUID uuid = player.getUniqueId();
        for (int index = 0; index < titles.size() && index < 45; index++) {
            TitleDefinition definition = titles.get(index);
            ItemStack icon = buildIcon(uuid, definition);
            inventory.setItem(9 + index, icon);
            currentView.add(definition);
        }
    }

    private void renderFilters() {
        inventory.setItem(0, filterToggleItem(Material.ITEM_FRAME, ownedOnly ? "§a획득 칭호만" : "§7전체 칭호", List.of(
                Component.text("§f클릭으로 " + (ownedOnly ? "전체 보기" : "획득만 보기") + " 전환"))));

        inventory.setItem(1, filterToggleItem(Material.NETHER_STAR, rarityFilterLabel(), List.of(
                Component.text("§7왼쪽 클릭: 다음 희귀도"),
                Component.text("§7셋팅: " + rarityFilterLabel()))));

        inventory.setItem(2, filterToggleItem(Material.BOOKSHELF, typeFilterLabel(), List.of(
                Component.text("§7왼쪽 클릭: 다음 타입"),
                Component.text("§7설정: " + typeFilterLabel()))));

        inventory.setItem(3, filterToggleItem(Material.NAME_TAG, "§f이름 필터: " + NAME_FILTERS[nameFilterIndex], List.of(
                Component.text("§7왼쪽 클릭: 다음 범위"))));

        inventory.setItem(8, filterToggleItem(Material.BARRIER, "§cGUI 닫기", List.of(Component.text("§7클릭 시 창을 닫습니다."))));

        for (int i = 4; i < 9; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, decorativePane());
            }
        }
    }

    private String rarityColor(TitleRarity rarity) {
        return switch (rarity) {
            case COMMON -> "§7";
            case UNCOMMON -> "§a";
            case RARE -> "§9";
            case EPIC -> "§5";
            case LEGENDARY -> "§6";
            case MYTHIC -> "§d";
        };
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() != player || !event.getInventory().equals(inventory)) {
            return;
        }
        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == 0) {
            ownedOnly = !ownedOnly;
            refresh();
            return;
        }
        if (slot == 1) {
            cycleRarity();
            refresh();
            return;
        }
        if (slot == 2) {
            cycleType();
            refresh();
            return;
        }
        if (slot == 3) {
            nameFilterIndex = (nameFilterIndex + 1) % NAME_FILTERS.length;
            refresh();
            return;
        }
        if (slot == 8) {
            player.closeInventory();
            return;
        }

        if (slot < 9) {
            return;
        }

        int index = slot - 9;
        if (index >= 0 && index < currentView.size()) {
            TitleDefinition definition = currentView.get(index);
            UUID uuid = player.getUniqueId();
            if (titleService.getOwnedTitles(uuid).contains(definition.getId())) {
                if (titleService.equipTitle(uuid, definition.getId())) {
                    player.sendMessage("§a칭호가 장착되었습니다: " + definition.getDisplay());
                    refresh();
                }
            } else {
                player.sendMessage("§c아직 획득하지 않은 칭호입니다.");
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() == player && event.getInventory().equals(inventory)) {
            HandlerList.unregisterAll(this);
        }
    }

    private boolean passesFilters(TitleDefinition definition) {
        if (rarityFilter != null && definition.getRarity() != rarityFilter) {
            return false;
        }
        if (typeFilter != null && !typeFilter.equalsIgnoreCase(definition.getType())) {
            return false;
        }
        if (ownedOnly && !titleService.getOwnedTitles(player.getUniqueId()).contains(definition.getId())) {
            return false;
        }
        return matchesNameFilter(definition);
    }

    private boolean matchesNameFilter(TitleDefinition definition) {
        if (nameFilterIndex == 0) {
            return true;
        }
        String plain = strip(definition.getDisplay());
        if (plain.isEmpty()) {
            return true;
        }
        char ch = Character.toUpperCase(plain.charAt(0));
        return switch (nameFilterIndex) {
            case 1 -> ch >= 'A' && ch <= 'F';
            case 2 -> ch >= 'G' && ch <= 'L';
            case 3 -> ch >= 'M' && ch <= 'R';
            case 4 -> ch >= 'S' && ch <= 'Z';
            case 5 -> !(ch >= 'A' && ch <= 'Z');
            default -> true;
        };
    }

    private void cycleRarity() {
        if (rarityFilter == null) {
            rarityFilter = RARITY_CYCLE[0];
            return;
        }
        int index = rarityFilter.ordinal() + 1;
        rarityFilter = index >= RARITY_CYCLE.length ? null : RARITY_CYCLE[index];
    }

    private void cycleType() {
        List<String> types = new ArrayList<>(collectTypes());
        if (types.isEmpty()) {
            typeFilter = null;
            return;
        }
        if (typeFilter == null) {
            typeFilter = types.get(0);
            return;
        }
        int idx = types.indexOf(typeFilter);
        if (idx == -1 || idx + 1 >= types.size()) {
            typeFilter = null;
        } else {
            typeFilter = types.get(idx + 1);
        }
    }

    private Set<String> collectTypes() {
        return plugin.getTitleRegistry().all().stream()
                .map(TitleDefinition::getType)
                .filter(type -> type != null && !type.isBlank())
                .map(type -> type.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ItemStack buildIcon(UUID uuid, TitleDefinition definition) {
        boolean owned = titleService.getOwnedTitles(uuid).contains(definition.getId());
        boolean equipped = titleService.getEquippedTitle(uuid).map(definition.getId()::equals).orElse(false);
        ItemStack stack;
        if (!owned) {
            stack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        } else if (equipped) {
            stack = new ItemStack(Material.ENCHANTED_BOOK);
        } else {
            stack = new ItemStack(Material.NAME_TAG);
        }
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(TextUtil.legacy((owned ? "§f" : "§c잠금: ") + strip(definition.getDisplay())));
        List<Component> lore = new ArrayList<>();
        lore.add(TextUtil.legacy("§f희귀도: " + rarityColor(definition.getRarity()) + definition.getRarity().name()));
        lore.add(TextUtil.legacy("§f타입: §7" + definition.getType()));
        if (definition.isSeasonal()) {
            lore.add(TextUtil.legacy("§b시즌 한정 칭호"));
        }
        if (definition.isWeeklyExclusive()) {
            lore.add(TextUtil.legacy("§d주간 랭킹 한정"));
        }
        TitleRequirement requirement = definition.getRequirement();
        if (!owned && requirement != null && requirement.getAmount() > 0) {
            long progress = requirementService.getProgress(uuid, definition.getId());
            lore.add(TextUtil.legacy("§f진행도: §a" + progress + "§7 / §e" + requirement.getAmount()));
            lore.add(TextUtil.legacy("§7조건: " + requirementLabel(requirement)));
        } else {
            lore.add(TextUtil.legacy("§f상태: " + (owned ? "§a획득" : "§c미획득")));
        }
        if (!definition.getEffects().isEmpty()) {
            lore.add(TextUtil.legacy("§6효과"));
            for (TitleEffect effect : definition.getEffects()) {
                lore.add(TextUtil.legacy(" §7- " + effect.getType().name()));
            }
        }
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        stack.setItemMeta(meta);
        return stack;
    }

    private String requirementLabel(TitleRequirement requirement) {
        return switch (requirement.getType()) {
            case SELL -> "판매 " + requirement.getMaterial();
            case BREAK -> "채집 " + requirement.getMaterial();
            case EVENT -> "이벤트 참여";
            case COMMAND -> "커맨드 해금";
        };
    }

    private ItemStack filterToggleItem(Material material, String name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(TextUtil.legacy(name));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack decorativePane() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    private String rarityFilterLabel() {
        return rarityFilter == null ? "§7희귀도: 전체" : "§f희귀도: " + rarityFilter.name();
    }

    private String typeFilterLabel() {
        return typeFilter == null ? "§7타입: 전체" : "§f타입: " + typeFilter;
    }

    private String strip(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("<[^>]+>", "");
    }
}
