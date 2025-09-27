package wiki.creeper.creeperPrefixSystem.data.player;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Cached runtime object representing the titles a player owns, the one they currently have equipped and
 * the timestamps the titles were obtained at.
 */
public final class PlayerTitleState {
    private final Set<String> ownedTitles = new HashSet<>();
    private final Set<String> seasonalTitles = new HashSet<>();
    private final Set<String> weeklyTitles = new HashSet<>();
    private String equippedTitle;
    private Instant lastSynced;

    public void addOwnedTitle(String titleId) {
        ownedTitles.add(titleId);
    }

    public boolean removeOwnedTitle(String titleId) {
        seasonalTitles.remove(titleId);
        weeklyTitles.remove(titleId);
        return ownedTitles.remove(titleId);
    }

    public boolean isOwned(String titleId) {
        return ownedTitles.contains(titleId);
    }

    public Set<String> getOwnedTitles() {
        return Collections.unmodifiableSet(ownedTitles);
    }

    public Optional<String> getEquippedTitle() {
        return Optional.ofNullable(equippedTitle);
    }

    public void setEquippedTitle(String equippedTitle) {
        this.equippedTitle = equippedTitle;
    }

    public Set<String> getSeasonalTitles() {
        return Collections.unmodifiableSet(seasonalTitles);
    }

    public void markSeasonal(String titleId) {
        seasonalTitles.add(titleId);
    }

    public void markWeekly(String titleId) {
        weeklyTitles.add(titleId);
    }

    public Set<String> getWeeklyTitles() {
        return Collections.unmodifiableSet(weeklyTitles);
    }

    public Instant getLastSynced() {
        return lastSynced;
    }

    public void setLastSynced(Instant lastSynced) {
        this.lastSynced = lastSynced;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerTitleState that = (PlayerTitleState) o;
        return Objects.equals(ownedTitles, that.ownedTitles) && Objects.equals(seasonalTitles, that.seasonalTitles) && Objects.equals(weeklyTitles, that.weeklyTitles) && Objects.equals(equippedTitle, that.equippedTitle) && Objects.equals(lastSynced, that.lastSynced);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownedTitles, seasonalTitles, weeklyTitles, equippedTitle, lastSynced);
    }
}
