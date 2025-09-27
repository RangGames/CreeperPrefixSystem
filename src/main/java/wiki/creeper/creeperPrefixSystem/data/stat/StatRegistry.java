package wiki.creeper.creeperPrefixSystem.data.stat;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Level;

/**
 * Loads stat definitions from YAML and exposes lookups for runtime calculations.
 */
public final class StatRegistry {

    private final Plugin plugin;
    private final Map<String, StatDefinition> stats = new HashMap<>();

    public StatRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration configuration) {
        stats.clear();
        ConfigurationSection root = configuration.getConfigurationSection("stats");
        if (root == null) {
            plugin.getLogger().warning("No stats section defined in stats.yml");
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                String display = section.getString("display", id);
                double min = section.getDouble("min", 0.0D);
                double max = section.getDouble("max", 0.0D);
                double def = section.getDouble("default", 0.0D);
                StatModifier.Operation stacking = StatModifier.Operation.valueOf(section.getString("stacking", "ADD").toUpperCase(Locale.ROOT));
                StatDefinition definition = new StatDefinition(id, display, min, max, def, stacking);
                stats.put(id, definition);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to parse stat " + id, ex);
            }
        }
        plugin.getLogger().info("Loaded " + stats.size() + " stats");
    }

    public StatDefinition get(String id) {
        return stats.get(id);
    }

    public Collection<StatDefinition> all() {
        return Collections.unmodifiableCollection(stats.values());
    }
}
