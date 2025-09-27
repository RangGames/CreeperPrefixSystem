package wiki.creeper.creeperPrefixSystem.data.set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import wiki.creeper.creeperPrefixSystem.data.stat.StatModifier;
import wiki.creeper.creeperPrefixSystem.data.title.TitleEffect;

import java.util.*;
import java.util.logging.Level;

/**
 * Loader for set definitions.
 */
public final class SetRegistry {

    private final Plugin plugin;
    private final Map<String, SetDefinition> sets = new HashMap<>();

    public SetRegistry(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load(FileConfiguration configuration) {
        sets.clear();
        ConfigurationSection root = configuration.getConfigurationSection("sets");
        if (root == null) {
            plugin.getLogger().warning("No sets section defined in sets.yml");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                String name = section.getString("name", id);
                List<String> members = section.getStringList("members");
                List<TitleEffect> effects = new ArrayList<>();
                for (Map<?, ?> raw : section.getMapList("effects")) {
                    String type = Objects.toString(raw.get("type"), "STAT_MOD");
                    if ("STAT_MOD".equalsIgnoreCase(type)) {
                        String statId = Objects.toString(raw.get("stat"), null);
                        StatModifier.Operation op = StatModifier.Operation.valueOf(Objects.toString(raw.get("op"), "ADD").toUpperCase(Locale.ROOT));
                        double value = Double.parseDouble(Objects.toString(raw.get("value"), "0"));
                        effects.add(TitleEffect.stat(statId, op, value));
                    }
                }
                sets.put(id, new SetDefinition(id, name, members, effects));
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to parse set " + id, ex);
            }
        }
        plugin.getLogger().info("Loaded " + sets.size() + " sets");
    }

    public Collection<SetDefinition> all() {
        return Collections.unmodifiableCollection(sets.values());
    }

    public SetDefinition get(String id) {
        return sets.get(id);
    }
}
