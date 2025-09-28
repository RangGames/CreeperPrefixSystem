package wiki.creeper.creeperPrefixSystem.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Material;
import wiki.creeper.creeperPrefixSystem.config.StorageConfig;
import wiki.creeper.creeperPrefixSystem.data.achievement.AchievementCompletion;
import wiki.creeper.creeperPrefixSystem.data.collection.CollectionEntry;
import wiki.creeper.creeperPrefixSystem.data.player.PlayerStatState;
import wiki.creeper.creeperPrefixSystem.data.player.PlayerTitleState;
import wiki.creeper.creeperPrefixSystem.data.stat.StatModifier;
import wiki.creeper.creeperPrefixSystem.data.ranking.WeeklyStanding;
import wiki.creeper.creeperPrefixSystem.data.season.SeasonSnapshot;
import wiki.creeper.creeperPrefixSystem.data.season.SeasonState;
import wiki.creeper.creeperPrefixSystem.util.UuidUtil;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles persistence operations for titles, stats, seasons and rankings using MySQL.
 */
public final class MySqlStorage implements AutoCloseable {

    private final Logger logger;
    private final StorageConfig config;
    private HikariDataSource dataSource;

    public MySqlStorage(Logger logger, StorageConfig config) {
        this.logger = logger;
        this.config = config;
    }

    public void init() throws SQLException {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mysql://" + config.host() + ":" + config.port() + "/" + config.database() + "?useSSL=false&serverTimezone=UTC");
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(config.maximumPoolSize());
        hikari.setMinimumIdle(config.minimumIdle());
        hikari.setConnectionTimeout(config.connectionTimeout());
        hikari.setPoolName("TitlePlus-Hikari");
        hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource = new HikariDataSource(hikari);
        createSchema();
    }

    private void createSchema() throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS titles (" +
                    "id VARCHAR(64) PRIMARY KEY," +
                    "display VARCHAR(256)," +
                    "rarity VARCHAR(16)," +
                    "type VARCHAR(32)," +
                    "skin_json TEXT," +
                    "effects_json TEXT," +
                    "seasonal TINYINT(1) DEFAULT 0," +
                    "weekly_exclusive TINYINT(1) DEFAULT 0" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS sets (" +
                    "id VARCHAR(64) PRIMARY KEY," +
                    "name VARCHAR(64)," +
                    "members_json TEXT," +
                    "effects_json TEXT" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_titles (" +
                    "uuid BINARY(16)," +
                    "title_id VARCHAR(64)," +
                    "obtained_at DATETIME," +
                    "equipped TINYINT(1)," +
                    "PRIMARY KEY(uuid, title_id)," +
                    "INDEX idx_player_titles_uuid(uuid)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS collection_entries (" +
                    "entry_id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "uuid BINARY(16)," +
                    "material VARCHAR(64)," +
                    "registered_at DATETIME," +
                    "player_rank INT," +
                    "UNIQUE KEY uq_collection_player_material(uuid, material)," +
                    "INDEX idx_collection_uuid(uuid)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS stat_defs (" +
                    "stat_id VARCHAR(64) PRIMARY KEY," +
                    "display VARCHAR(128)," +
                    "min DOUBLE," +
                    "max DOUBLE," +
                    "default_val DOUBLE," +
                    "stacking VARCHAR(16)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_stats (" +
                    "uuid BINARY(16)," +
                    "stat_id VARCHAR(64)," +
                    "base_value DOUBLE," +
                    "PRIMARY KEY(uuid, stat_id)," +
                    "INDEX idx_player_stats_uuid(uuid)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS stat_modifiers (" +
                    "uuid BINARY(16)," +
                    "stat_id VARCHAR(64)," +
                    "source_id VARCHAR(64)," +
                    "op VARCHAR(8)," +
                    "value DOUBLE," +
                    "expire_at BIGINT," +
                    "PRIMARY KEY(uuid, stat_id, source_id)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS seasons (" +
                    "season_id INT PRIMARY KEY AUTO_INCREMENT," +
                    "name VARCHAR(64)," +
                    "start_at DATETIME," +
                    "end_at DATETIME," +
                    "state VARCHAR(16)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS weekly_metrics (" +
                    "week_key CHAR(8)," +
                    "uuid BINARY(16)," +
                    "metric VARCHAR(32)," +
                    "value BIGINT," +
                    "PRIMARY KEY(week_key, uuid, metric)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS weekly_rank_awards (" +
                    "week_key CHAR(8)," +
                    "rank_position INT," +
                    "uuid BINARY(16)," +
                    "title_id VARCHAR(64)," +
                    "awarded_at DATETIME," +
                    "PRIMARY KEY(week_key, rank_position)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS title_progress (" +
                    "uuid BINARY(16)," +
                    "title_id VARCHAR(64)," +
                    "progress BIGINT," +
                    "PRIMARY KEY(uuid, title_id)" +
                    ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS achievement_completions (" +
                    "completion_id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "uuid BINARY(16)," +
                    "achievement_id VARCHAR(64)," +
                    "completed_at DATETIME," +
                    "UNIQUE KEY uq_achievement_player(uuid, achievement_id)," +
                    "INDEX idx_achievement_uuid(uuid)" +
                    ")");
        }
    }

    public void loadPlayerTitles(UUID uuid, PlayerTitleState state) {
        String sql = "SELECT title_id, equipped FROM player_titles WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String titleId = rs.getString("title_id");
                    state.addOwnedTitle(titleId);
                    if (rs.getBoolean("equipped")) {
                        state.setEquippedTitle(titleId);
                    }
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to query player titles", ex);
        }
    }

    public void upsertPlayerTitle(UUID uuid, String titleId, boolean equipped) {
        String sql = "INSERT INTO player_titles(uuid, title_id, obtained_at, equipped) VALUES(?,?,NOW(),?) " +
                "ON DUPLICATE KEY UPDATE equipped = VALUES(equipped)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            ps.setString(2, titleId);
            ps.setBoolean(3, equipped);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to upsert player title", ex);
        }
    }

    public void clearEquipped(UUID uuid) {
        String sql = "UPDATE player_titles SET equipped = 0 WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to clear equipped title", ex);
        }
    }

    public void deletePlayerTitle(UUID uuid, String titleId) {
        String sql = "DELETE FROM player_titles WHERE uuid = ? AND title_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            ps.setString(2, titleId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to delete player title", ex);
        }
    }

    public List<CollectionEntry> loadCollectionEntries(UUID uuid) {
        List<CollectionEntry> entries = new ArrayList<>();
        String sql = "SELECT entry_id, material, registered_at, player_rank FROM collection_entries WHERE uuid = ? ORDER BY player_rank ASC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String materialName = rs.getString("material");
                    Material material = Material.matchMaterial(materialName);
                    if (material == null) {
                        continue;
                    }
                    Timestamp timestamp = rs.getTimestamp("registered_at");
                    Instant registeredAt = timestamp == null ? Instant.EPOCH : timestamp.toInstant();
                    int playerRank = rs.getInt("player_rank");
                    long globalRank = rs.getLong("entry_id");
                    entries.add(new CollectionEntry(material, registeredAt, playerRank, globalRank));
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to load collection entries", ex);
        }
        return entries;
    }

    public long insertCollectionEntry(UUID uuid, CollectionEntry entry) {
        String sql = "INSERT INTO collection_entries(uuid, material, registered_at, player_rank) VALUES(?,?,?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            ps.setString(2, entry.getMaterial().name());
            ps.setTimestamp(3, Timestamp.from(entry.getRegisteredAt()));
            ps.setInt(4, entry.getPlayerRank());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException ex) {
            if (!isDuplicate(ex)) {
                logger.log(Level.SEVERE, "Failed to insert collection entry", ex);
            }
        }
        return 0L;
    }

    public List<AchievementCompletion> loadAchievementCompletions(UUID uuid) {
        List<AchievementCompletion> completions = new ArrayList<>();
        String sql = "SELECT completion_id, achievement_id, completed_at FROM achievement_completions WHERE uuid = ? ORDER BY completed_at ASC";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String achievementId = rs.getString("achievement_id");
                    Timestamp timestamp = rs.getTimestamp("completed_at");
                    Instant completedAt = timestamp == null ? Instant.EPOCH : timestamp.toInstant();
                    long globalRank = rs.getLong("completion_id");
                    completions.add(new AchievementCompletion(achievementId, completedAt, globalRank));
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to load achievement completions", ex);
        }
        return completions;
    }

    public long insertAchievementCompletion(UUID uuid, AchievementCompletion completion) {
        String sql = "INSERT INTO achievement_completions(uuid, achievement_id, completed_at) VALUES(?,?,?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            ps.setString(2, completion.getAchievementId());
            ps.setTimestamp(3, Timestamp.from(completion.getCompletedAt()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        } catch (SQLException ex) {
            if (!isDuplicate(ex)) {
                logger.log(Level.SEVERE, "Failed to insert achievement completion", ex);
            }
        }
        return 0L;
    }

    public void loadPlayerStats(UUID uuid, PlayerStatState stats) {
        String baseSql = "SELECT stat_id, base_value FROM player_stats WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(baseSql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stats.setBaseValue(rs.getString("stat_id"), rs.getDouble("base_value"));
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to load base stats", ex);
        }

        String modSql = "SELECT stat_id, source_id, op, value, expire_at FROM stat_modifiers WHERE uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(modSql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String statId = rs.getString("stat_id");
                    String sourceId = rs.getString("source_id");
                    StatModifier.Operation op = StatModifier.Operation.valueOf(rs.getString("op"));
                    double value = rs.getDouble("value");
                    long expireMillis = rs.getLong("expire_at");
                    Instant expire = rs.wasNull() ? null : Instant.ofEpochMilli(expireMillis);
                    stats.putModifier(new StatModifier(uuid, statId, sourceId, op, value, expire));
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to load stat modifiers", ex);
        }
    }

    public void upsertStatModifier(StatModifier modifier) {
        String sql = "INSERT INTO stat_modifiers(uuid, stat_id, source_id, op, value, expire_at) VALUES(?,?,?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE op = VALUES(op), value = VALUES(value), expire_at = VALUES(expire_at)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(modifier.getPlayerId()));
            ps.setString(2, modifier.getStatId());
            ps.setString(3, modifier.getSourceId());
            ps.setString(4, modifier.getOperation().name());
            ps.setDouble(5, modifier.getValue());
            if (modifier.getExpireAt() == null) {
                ps.setNull(6, Types.BIGINT);
            } else {
                ps.setLong(6, modifier.getExpireAt().toEpochMilli());
            }
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to upsert stat modifier", ex);
        }
    }

    public void removeStatModifier(UUID uuid, String statId, String sourceId) {
        String sql = "DELETE FROM stat_modifiers WHERE uuid = ? AND stat_id = ? AND source_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            ps.setString(2, statId);
            ps.setString(3, sourceId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to remove stat modifier", ex);
        }
    }

    public void saveBaseStat(UUID uuid, String statId, double value) {
        String sql = "INSERT INTO player_stats(uuid, stat_id, base_value) VALUES(?,?,?) " +
                "ON DUPLICATE KEY UPDATE base_value = VALUES(base_value)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            ps.setString(2, statId);
            ps.setDouble(3, value);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to store base stat", ex);
        }
    }

    public SeasonSnapshot loadLatestSeason() {
        String sql = "SELECT season_id, name, start_at, end_at, state FROM seasons ORDER BY season_id DESC LIMIT 1";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int id = rs.getInt("season_id");
                String name = rs.getString("name");
                Timestamp startTs = rs.getTimestamp("start_at");
                Timestamp endTs = rs.getTimestamp("end_at");
                String stateStr = rs.getString("state");
                SeasonState state = stateStr == null ? SeasonState.PREPARING : SeasonState.valueOf(stateStr);
                return new SeasonSnapshot(id,
                        name,
                        startTs == null ? null : startTs.toInstant(),
                        endTs == null ? null : endTs.toInstant(),
                        state);
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to load latest season", ex);
        }
        return null;
    }

    public SeasonSnapshot createSeason(String name, SeasonState state) {
        String sql = "INSERT INTO seasons(name, start_at, state) VALUES(?, NOW(), ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, state.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    return new SeasonSnapshot(id, name, Instant.now(), null, state);
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to create season", ex);
        }
        return null;
    }

    public void updateSeasonState(int seasonId, SeasonState state) {
        String sql = "UPDATE seasons SET state = ? WHERE season_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, state.name());
            ps.setInt(2, seasonId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to update season state", ex);
        }
    }

    public void incrementWeeklyMetric(String weekKey, UUID uuid, String metric, long delta) {
        String sql = "INSERT INTO weekly_metrics(week_key, uuid, metric, value) VALUES(?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE value = value + VALUES(value)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, weekKey);
            ps.setBytes(2, UuidUtil.toBytes(uuid));
            ps.setString(3, metric);
            ps.setLong(4, delta);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to upsert weekly metric", ex);
        }
    }

    public List<WeeklyStanding> loadWeeklyStandings(String weekKey, String metric, int limit) {
        String sql = "SELECT uuid, value FROM weekly_metrics WHERE week_key = ? AND metric = ? ORDER BY value DESC LIMIT ?";
        List<WeeklyStanding> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, weekKey);
            ps.setString(2, metric);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byte[] uuidBytes = rs.getBytes("uuid");
                    long value = rs.getLong("value");
                    result.add(new WeeklyStanding(UuidUtil.fromBytes(uuidBytes), metric, value));
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to load weekly standings", ex);
        }
        return result;
    }

    public void saveWeeklyAward(String weekKey, int rank, UUID uuid, String titleId) {
        String sql = "INSERT INTO weekly_rank_awards(week_key, rank_position, uuid, title_id, awarded_at) VALUES(?,?,?,?,NOW()) " +
                "ON DUPLICATE KEY UPDATE uuid = VALUES(uuid), title_id = VALUES(title_id), awarded_at = NOW()";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, weekKey);
            ps.setInt(2, rank);
            ps.setBytes(3, UuidUtil.toBytes(uuid));
            ps.setString(4, titleId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to store weekly award", ex);
        }
    }

    public Map<String, Long> loadTitleProgress(UUID uuid) {
        String sql = "SELECT title_id, progress FROM title_progress WHERE uuid = ?";
        Map<String, Long> progress = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    progress.put(rs.getString("title_id"), rs.getLong("progress"));
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to load title progress", ex);
        }
        return progress;
    }

    public void setTitleProgress(UUID uuid, String titleId, long value) {
        String sql = "INSERT INTO title_progress(uuid, title_id, progress) VALUES(?,?,?) " +
                "ON DUPLICATE KEY UPDATE progress = VALUES(progress)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            ps.setString(2, titleId);
            ps.setLong(3, value);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to store title progress", ex);
        }
    }

    public void incrementTitleProgress(UUID uuid, String titleId, long delta) {
        String sql = "INSERT INTO title_progress(uuid, title_id, progress) VALUES(?,?,?) " +
                "ON DUPLICATE KEY UPDATE progress = progress + VALUES(progress)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, UuidUtil.toBytes(uuid));
            ps.setString(2, titleId);
            ps.setLong(3, delta);
            ps.executeUpdate();
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Failed to increment title progress", ex);
        }
    }

    private boolean isDuplicate(SQLException exception) {
        return "23000".equals(exception.getSQLState());
    }

    @Override
    public void close() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Failed to close datasource", ex);
            }
        }
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }
}
