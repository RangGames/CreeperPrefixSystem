package wiki.creeper.creeperPrefixSystem.data.collection;

import org.bukkit.Material;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks the collection state for a single player in-memory.
 */
public final class PlayerCollectionState {

    private final Map<Material, CollectionEntry> entries = new LinkedHashMap<>();

    public Collection<CollectionEntry> getEntries() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public boolean hasEntry(Material material) {
        return entries.containsKey(material);
    }

    public void addEntry(CollectionEntry entry) {
        entries.put(entry.getMaterial(), entry);
    }

    public CollectionEntry getEntry(Material material) {
        return entries.get(material);
    }

    public int size() {
        return entries.size();
    }
}
