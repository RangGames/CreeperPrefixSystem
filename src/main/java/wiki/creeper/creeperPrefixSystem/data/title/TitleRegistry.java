package wiki.creeper.creeperPrefixSystem.data.title;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.Plugin;
import wiki.creeper.creeperPrefixSystem.data.stat.StatModifier;

import java.util.*;
import java.util.logging.Level;

/**
 * Loads and caches {@link TitleDefinition} instances from YAML configuration.
 */
public final class TitleRegistry {

    private final Plugin plugin;
    private final Map<String, TitleDefinition> titles = new HashMap<>();

    public TitleRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration configuration) {
        titles.clear();
        ConfigurationSection root = configuration.getConfigurationSection("titles");
        if (root == null) {
            plugin.getLogger().warning("No titles section defined in titles.yml");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                TitleDefinition definition = parseTitle(id, section);
                titles.put(id, definition);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to parse title " + id, ex);
            }
        }
        plugin.getLogger().info("Loaded " + titles.size() + " titles");
    }

    private TitleDefinition parseTitle(String id, ConfigurationSection section) {
        String display = section.getString("display", id);
        TitleRarity rarity = TitleRarity.valueOf(section.getString("rarity", "COMMON").toUpperCase(Locale.ROOT));
        String type = section.getString("type", "GENERAL");
        boolean seasonal = section.getBoolean("seasonal", false);
        boolean weekly = section.getBoolean("weekly", false);

        TitleRequirement requirement = TitleRequirement.none();
        ConfigurationSection reqSection = section.getConfigurationSection("requirements");
        if (reqSection != null) {
            String reqType = reqSection.getString("type", "EVENT");
            TitleRequirement.Type typeEnum = TitleRequirement.Type.valueOf(reqType.toUpperCase(Locale.ROOT));
            Material material = null;
            String materialName = reqSection.getString("material");
            if (materialName != null) {
                material = Material.matchMaterial(materialName.toUpperCase(Locale.ROOT));
            }
            long amount = reqSection.getLong("amount", 0);
            String meta = reqSection.getString("meta");
            requirement = TitleRequirement.of(typeEnum, material, amount, meta);
        }

        List<TitleEffect> effects = new ArrayList<>();
        for (Map<?, ?> raw : section.getMapList("effects")) {
            String effectType = Objects.toString(raw.get("type"), "");
            if (effectType.isEmpty()) {
                continue;
            }
            switch (effectType.toUpperCase(Locale.ROOT)) {
                case "STAT_MOD" -> {
                    String statId = Objects.toString(raw.get("stat"), null);
                    if (statId == null) {
                        plugin.getLogger().warning("Stat modifier for title " + id + " is missing stat id.");
                        continue;
                    }
                    StatModifier.Operation op = StatModifier.Operation.valueOf(Objects.toString(raw.get("op"), "ADD").toUpperCase(Locale.ROOT));
                    double value = Double.parseDouble(Objects.toString(raw.get("value"), "0"));
                    effects.add(TitleEffect.stat(statId, op, value));
                }
                case "POTION" -> {
                    String potion = Objects.toString(raw.get("potion"), "SPEED");
                    PotionEffectType typeEnum = PotionEffectType.getByName(potion.toUpperCase(Locale.ROOT));
                    if (typeEnum == null) {
                        plugin.getLogger().warning("Unknown potion type " + potion + " for title " + id);
                        continue;
                    }
                    int level = Integer.parseInt(Objects.toString(raw.get("level"), "1"));
                    effects.add(TitleEffect.potion(typeEnum, level));
                }
                case "COMMAND" -> {
                    String command = Objects.toString(raw.get("command"), "");
                    if (!command.isEmpty()) {
                        effects.add(TitleEffect.command(command));
                    }
                }
                default -> plugin.getLogger().warning("Unsupported effect type " + effectType + " in title " + id);
            }
        }

        TitleSkin skin = null;
        ConfigurationSection skinSection = section.getConfigurationSection("skin");
        if (skinSection != null) {
            skin = new TitleSkin(skinSection.getString("prefix"), skinSection.getString("nametag"));
        }

        return new TitleDefinition(id, display, rarity, type, requirement, effects, skin, seasonal, weekly);
    }

    public TitleDefinition get(String id) {
        return titles.get(id);
    }

    public Collection<TitleDefinition> all() {
        return Collections.unmodifiableCollection(titles.values());
    }
}
