package wiki.creeper.creeperPrefixSystem.data.season;

import java.time.Instant;

/**
 * Immutable representation of a season fetched from storage.
 */
public record SeasonSnapshot(int id,
                             String name,
                             Instant startAt,
                             Instant endAt,
                             SeasonState state) {
}
