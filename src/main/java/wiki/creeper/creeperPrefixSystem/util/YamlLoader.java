package wiki.creeper.creeperPrefixSystem.util;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Utility to copy default YAML files from the plugin jar to the data directory and load them as
 * {@link FileConfiguration} instances.
 */
public final class YamlLoader {

    private YamlLoader() {
    }

    public static FileConfiguration loadOrCopy(JavaPlugin plugin, String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (!file.exists()) {
            plugin.saveResource(path, false);
        }
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Failed to load YAML file " + path + ": " + e.getMessage());
        }
        return configuration;
    }
}
