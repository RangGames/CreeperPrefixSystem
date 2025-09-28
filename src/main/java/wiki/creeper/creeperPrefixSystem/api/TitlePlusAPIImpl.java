package wiki.creeper.creeperPrefixSystem.api;

import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementCompletion;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementDefinition;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementRegistry;
import wiki.creeper.creeperPrefixSystem.data.collection.CollectionEntry;
import wiki.creeper.creeperPrefixSystem.data.season.SeasonState;
import wiki.creeper.creeperPrefixSystem.data.set.SetDefinition;
import wiki.creeper.creeperPrefixSystem.data.set.SetRegistry;
import wiki.creeper.creeperPrefixSystem.data.stat.StatDefinition;
import wiki.creeper.creeperPrefixSystem.data.stat.StatModifier;
import wiki.creeper.creeperPrefixSystem.data.stat.StatRegistry;
import wiki.creeper.creeperPrefixSystem.data.title.TitleDefinition;
import wiki.creeper.creeperPrefixSystem.data.title.TitleRegistry;
import wiki.creeper.creeperPrefixSystem.service.AchievementService;
import wiki.creeper.creeperPrefixSystem.service.CollectionService;
import wiki.creeper.creeperPrefixSystem.service.RequirementService;
import wiki.creeper.creeperPrefixSystem.service.SeasonService;
import wiki.creeper.creeperPrefixSystem.service.StatService;
import wiki.creeper.creeperPrefixSystem.service.TitleService;
import wiki.creeper.creeperPrefixSystem.service.WeeklyRankingService;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation bridging internal services to the public API contract.
 */
public final class TitlePlusAPIImpl implements TitlePlusAPI {

    private final StatService statService;
    private final TitleService titleService;
    private final WeeklyRankingService weeklyRankingService;
    private final SeasonService seasonService;
    private final TitleRegistry titleRegistry;
    private final SetRegistry setRegistry;
    private final StatRegistry statRegistry;
    private final RequirementService requirementService;
    private final CollectionService collectionService;
    private final AchievementService achievementService;
    private final AchievementRegistry achievementRegistry;

    public TitlePlusAPIImpl(StatService statService,
                            TitleService titleService,
                            WeeklyRankingService weeklyRankingService,
                            SeasonService seasonService,
                            TitleRegistry titleRegistry,
                            SetRegistry setRegistry,
                            StatRegistry statRegistry,
                            RequirementService requirementService,
                            CollectionService collectionService,
                            AchievementService achievementService,
                            AchievementRegistry achievementRegistry) {
        this.statService = statService;
        this.titleService = titleService;
        this.weeklyRankingService = weeklyRankingService;
        this.seasonService = seasonService;
        this.titleRegistry = titleRegistry;
        this.setRegistry = setRegistry;
        this.statRegistry = statRegistry;
        this.requirementService = requirementService;
        this.collectionService = collectionService;
        this.achievementService = achievementService;
        this.achievementRegistry = achievementRegistry;
    }

    @Override
    public double getStat(@NotNull UUID uuid, @NotNull String statId) {
        return statService.getStat(uuid, statId);
    }

    @Override
    public void addModifier(@NotNull UUID uuid,
                            @NotNull String statId,
                            double value,
                            @NotNull String sourceId,
                            StatModifier.Operation operation,
                            Long expireAt) {
        statService.addModifier(uuid, statId, value, sourceId, operation == null ? StatModifier.Operation.ADD : operation, expireAt);
    }

    @Override
    public boolean removeModifier(@NotNull UUID uuid, @NotNull String statId, @NotNull String sourceId) {
        return statService.removeModifier(uuid, statId, sourceId);
    }

    @Override
    public boolean grantTitle(@NotNull UUID uuid, @NotNull String titleId) {
        return titleService.grantTitle(uuid, titleId);
    }

    @Override
    public boolean equipTitle(@NotNull UUID uuid, @NotNull String titleId) {
        return titleService.equipTitle(uuid, titleId);
    }

    @Override
    public boolean unequipTitle(@NotNull UUID uuid) {
        return titleService.unequip(uuid);
    }

    @Override
    public boolean isWeeklyTop3(@NotNull UUID uuid) {
        return weeklyRankingService.isTop3(uuid);
    }

    @Override
    public @NotNull SeasonState getSeasonState() {
        return seasonService.getState();
    }

    @Override
    public @NotNull Optional<String> getEquippedTitle(@NotNull UUID uuid) {
        return titleService.getEquippedTitle(uuid);
    }

    @Override
    public @NotNull Collection<String> getOwnedTitles(@NotNull UUID uuid) {
        Set<String> owned = titleService.getOwnedTitles(uuid);
        return Set.copyOf(owned);
    }

    @Override
    public void invalidatePlayer(@NotNull OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        titleService.unload(uuid);
        statService.unload(uuid);
        requirementService.unload(uuid);
        collectionService.unload(uuid);
        achievementService.unload(uuid);
    }

    @Override
    public @NotNull Optional<TitleDefinition> getTitleDefinition(@NotNull String titleId) {
        return Optional.ofNullable(titleRegistry.get(titleId));
    }

    @Override
    public @NotNull Collection<TitleDefinition> getTitleDefinitions() {
        return titleRegistry.all();
    }

    @Override
    public @NotNull Optional<SetDefinition> getSetDefinition(@NotNull String setId) {
        return Optional.ofNullable(setRegistry.get(setId));
    }

    @Override
    public @NotNull Collection<SetDefinition> getSetDefinitions() {
        return setRegistry.all();
    }

    @Override
    public @NotNull Optional<StatDefinition> getStatDefinition(@NotNull String statId) {
        return Optional.ofNullable(statRegistry.get(statId));
    }

    @Override
    public @NotNull Collection<StatDefinition> getStatDefinitions() {
        return statRegistry.all();
    }

    @Override
    public void incrementWeeklyMetric(@NotNull UUID uuid, @NotNull String metric, long delta) {
        weeklyRankingService.incrementMetric(uuid, metric, delta);
    }

    @Override
    public void addRequirementProgress(@NotNull UUID uuid, @NotNull String titleId, long amount) {
        requirementService.addProgress(uuid, titleId, amount);
    }

    @Override
    public long getRequirementProgress(@NotNull UUID uuid, @NotNull String titleId) {
        return requirementService.getProgress(uuid, titleId);
    }

    @Override
    public void recordSale(@NotNull UUID uuid, @NotNull Material material, long amount) {
        requirementService.handleSell(uuid, material, amount);
    }

    @Override
    public boolean registerCollection(@NotNull UUID uuid, @NotNull Material material, boolean grantXp, boolean notifyPlayer) {
        return collectionService.registerEntry(uuid, material, grantXp, notifyPlayer);
    }

    @Override
    public @NotNull Collection<CollectionEntry> getCollectionEntries(@NotNull UUID uuid) {
        return collectionService.getEntries(uuid);
    }

    @Override
    public boolean hasCollectionEntry(@NotNull UUID uuid, @NotNull Material material) {
        return collectionService.hasEntry(uuid, material);
    }

    @Override
    public @NotNull Collection<AchievementCompletion> getAchievementCompletions(@NotNull UUID uuid) {
        return achievementService.getCompletions(uuid);
    }

    @Override
    public boolean hasAchievement(@NotNull UUID uuid, @NotNull String achievementId) {
        return achievementService.hasCompletion(uuid, achievementId);
    }

    @Override
    public @NotNull Collection<AchievementDefinition> getAchievementDefinitions() {
        return achievementRegistry.all();
    }

    @Override
    public @NotNull Optional<AchievementDefinition> getAchievementDefinition(@NotNull String achievementId) {
        return Optional.ofNullable(achievementRegistry.get(achievementId));
    }
}
