package wiki.creeper.creeperPrefixSystem.data.achievement;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores loaded achievement definitions and exposes lookup helpers.
 */
public final class AchievementRegistry {

    private final Map<String, AchievementDefinition> definitions = new LinkedHashMap<>();
    private final Map<AchievementType, List<AchievementDefinition>> byType = new EnumMap<>(AchievementType.class);

    public void load(FileConfiguration configuration) {
        definitions.clear();
        byType.clear();
        ConfigurationSection root = configuration.getConfigurationSection("achievements");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            String display = section.getString("display", id);
            String description = section.getString("description", "");
            AchievementType type = AchievementType.fromString(section.getString("type"), AchievementType.COLLECTION_COUNT);
            int target = section.getInt("target", section.getInt("amount", 0));
            AchievementDefinition definition = new AchievementDefinition(id, display, description, type, target);
            definitions.put(id, definition);
            byType.computeIfAbsent(type, key -> new ArrayList<>()).add(definition);
        }
        byType.values().forEach(list -> list.sort((a, b) -> Integer.compare(a.getTarget(), b.getTarget())));
    }

    public AchievementDefinition get(String id) {
        return definitions.get(id);
    }

    public Collection<AchievementDefinition> all() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public List<AchievementDefinition> getByType(AchievementType type) {
        return byType.getOrDefault(type, List.of());
    }
}
