package wiki.creeper.creeperPrefixSystem.event;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Event;
import wiki.creeper.creeperPrefixSystem.data.ranking.WeeklyStanding;

import java.util.Collections;
import java.util.List;

/**
 * Fired after weekly standings have been computed, allowing subscribers to inspect or mutate the results
 * prior to caching/broadcasting.
 */
public final class WeeklyRankEvaluateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final String metric;
    private final String weekKey;
    private final List<WeeklyStanding> standings;

    public WeeklyRankEvaluateEvent(String metric, String weekKey, List<WeeklyStanding> standings, boolean async) {
        super(async);
        this.metric = metric;
        this.weekKey = weekKey;
        this.standings = standings;
    }

    public String getMetric() {
        return metric;
    }

    public String getWeekKey() {
        return weekKey;
    }

    public List<WeeklyStanding> getStandings() {
        return standings;
    }

    public List<WeeklyStanding> getUnmodifiableStandings() {
        return Collections.unmodifiableList(standings);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
