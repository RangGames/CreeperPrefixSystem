package wiki.creeper.creeperPrefixSystem.api;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementCompletion;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementDefinition;
import wiki.creeper.creeperPrefixSystem.data.collection.CollectionEntry;
import wiki.creeper.creeperPrefixSystem.data.season.SeasonState;
import wiki.creeper.creeperPrefixSystem.data.set.SetDefinition;
import wiki.creeper.creeperPrefixSystem.data.stat.StatDefinition;
import wiki.creeper.creeperPrefixSystem.data.stat.StatModifier;
import wiki.creeper.creeperPrefixSystem.data.title.TitleDefinition;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API exposed via Bukkit's {@link org.bukkit.plugin.ServicesManager} so external plugins can
 * interact with the TitlePlus systems without depending on internal implementation classes.
 */
public interface TitlePlusAPI {

    /**
     * Fetches a computed stat value for the provided player by applying base value and all modifiers.
     *
     * @param uuid   player unique id
     * @param statId stat identifier
     * @return computed stat value or the default definition if the player has none
     */
    double getStat(@NotNull UUID uuid, @NotNull String statId);

    /**
     * Adds a modifier to the player's statistic. When {@code expireAt} is null the modifier is persistent
     * until explicitly removed.
     */
    void addModifier(@NotNull UUID uuid,
                     @NotNull String statId,
                     double value,
                     @NotNull String sourceId,
                     @Nullable StatModifier.Operation operation,
                     @Nullable Long expireAt);

    /**
     * Removes a modifier that was previously added for the player.
     */
    boolean removeModifier(@NotNull UUID uuid, @NotNull String statId, @NotNull String sourceId);

    /**
     * Grants a title to the player. Returns {@code true} when a new grant is recorded.
     */
    boolean grantTitle(@NotNull UUID uuid, @NotNull String titleId);

    /**
     * Equips the specified title for the player if it is owned and active. The caller should ensure the
     * player is currently online for best feedback, but the system will persist the state regardless.
     */
    boolean equipTitle(@NotNull UUID uuid, @NotNull String titleId);

    /**
     * Unequips the current title for the player and removes all related effects.
     */
    boolean unequipTitle(@NotNull UUID uuid);

    /**
     * Queries whether the player is currently part of the weekly top3 cache.
     */
    boolean isWeeklyTop3(@NotNull UUID uuid);

    /**
     * @return the season state synchronized from the proxy or persisted storage.
     */
    @NotNull SeasonState getSeasonState();

    /**
     * @return optional title id the player currently has equipped.
     */
    @NotNull Optional<String> getEquippedTitle(@NotNull UUID uuid);

    /**
     * @return immutable collection of owned title identifiers for the player or empty when unknown.
     */
    @NotNull Collection<String> getOwnedTitles(@NotNull UUID uuid);

    /**
     * Attempts to flush any cached state for the supplied offline player.
     */
    void invalidatePlayer(@NotNull OfflinePlayer player);

    /**
     * Resolves an immutable title definition from the in-memory registry if available.
     */
    @NotNull Optional<TitleDefinition> getTitleDefinition(@NotNull String titleId);

    /**
     * @return all loaded title definitions.
     */
    @NotNull Collection<TitleDefinition> getTitleDefinitions();

    /**
     * Resolves a set bonus definition.
     */
    @NotNull Optional<SetDefinition> getSetDefinition(@NotNull String setId);

    @NotNull Collection<SetDefinition> getSetDefinitions();

    /**
     * Resolves a stat definition.
     */
    @NotNull Optional<StatDefinition> getStatDefinition(@NotNull String statId);

    @NotNull Collection<StatDefinition> getStatDefinitions();

    /**
     * Increments a weekly metric counter for the player.
     */
    void incrementWeeklyMetric(@NotNull UUID uuid, @NotNull String metric, long delta);

    /**
     * Adds progress towards a title requirement manually.
     */
    void addRequirementProgress(@NotNull UUID uuid, @NotNull String titleId, long amount);

    /**
     * Returns the stored requirement progress for a title.
     */
    long getRequirementProgress(@NotNull UUID uuid, @NotNull String titleId);

    /**
     * Convenience helper for SELL requirements.
     */
    void recordSale(@NotNull UUID uuid, @NotNull Material material, long amount);

    /**
     * Registers a collection entry forcibly, bypassing the natural acquisition chance.
     */
    default boolean registerCollection(@NotNull UUID uuid, @NotNull Material material) {
        return registerCollection(uuid, material, true, true);
    }

    boolean registerCollection(@NotNull UUID uuid, @NotNull Material material, boolean grantXp, boolean notifyPlayer);

    @NotNull Collection<CollectionEntry> getCollectionEntries(@NotNull UUID uuid);

    boolean hasCollectionEntry(@NotNull UUID uuid, @NotNull Material material);

    @NotNull Collection<AchievementCompletion> getAchievementCompletions(@NotNull UUID uuid);

    boolean hasAchievement(@NotNull UUID uuid, @NotNull String achievementId);

    @NotNull Collection<AchievementDefinition> getAchievementDefinitions();

    @NotNull Optional<AchievementDefinition> getAchievementDefinition(@NotNull String achievementId);
}
