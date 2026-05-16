package com.cinescoop.dao;

import com.cinescoop.database.DatabaseConnection;
import com.cinescoop.model.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RatingDAO {

    private static final Logger logger = LoggerFactory.getLogger(RatingDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public int insertBatch(List<Rating> ratings) {
        String sql = """
            INSERT INTO ratings (rating_id, movie_id, user_id, rating_value, review,
                review_date, likes, dislikes, watch_completed, device_used,
                watch_duration, favorite_scene, recommendation, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE rating_value = VALUES(rating_value)
            """;
        int total = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (Rating r : ratings) {
                ps.setInt(1, r.getRatingId());
                ps.setInt(2, r.getMovieId());
                ps.setInt(3, r.getUserId());
                ps.setFloat(4, r.getRatingValue());
                ps.setString(5, r.getReview());
                ps.setObject(6, r.getReviewDate(), Types.DATE);
                ps.setInt(7, r.getLikes());
                ps.setInt(8, r.getDislikes());
                ps.setBoolean(9, r.isWatchCompleted());
                ps.setString(10, r.getDeviceUsed());
                ps.setObject(11, r.getWatchDuration(), Types.INTEGER);
                ps.setString(12, r.getFavoriteScene());
                ps.setBoolean(13, r.isRecommendation());
                ps.setObject(14, r.getCreatedAt(), Types.TIMESTAMP);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            conn.commit();
            for (int r : results) total += (r >= 0 ? r : 0);
            logger.info("Batch inserted {} ratings.", total);
        } catch (SQLException e) {
            logger.error("Batch insert ratings failed: {}", e.getMessage());
        }
        return total;
    }

    public List<Rating> findAll() {
        List<Rating> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM ratings ORDER BY rating_id")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("findAll ratings failed: {}", e.getMessage());
        }
        return list;
    }

    public List<Rating> findByMovieId(int movieId) {
        List<Rating> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM ratings WHERE movie_id = ? ORDER BY created_at DESC")) {
            ps.setInt(1, movieId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("findByMovieId ratings failed: {}", e.getMessage());
        }
        return list;
    }

    public int count() {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM ratings")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("count ratings failed: {}", e.getMessage());
        }
        return 0;
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setRatingId(rs.getInt("rating_id"));
        r.setMovieId(rs.getInt("movie_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setRatingValue(rs.getFloat("rating_value"));
        r.setReview(rs.getString("review"));
        Date rd = rs.getDate("review_date");
        r.setReviewDate(rd != null ? rd.toLocalDate() : null);
        r.setLikes(rs.getInt("likes"));
        r.setDislikes(rs.getInt("dislikes"));
        r.setWatchCompleted(rs.getBoolean("watch_completed"));
        r.setDeviceUsed(rs.getString("device_used"));
        int wd = rs.getInt("watch_duration");
        r.setWatchDuration(rs.wasNull() ? null : wd);
        r.setFavoriteScene(rs.getString("favorite_scene"));
        r.setRecommendation(rs.getBoolean("recommendation"));
        Timestamp ts = rs.getTimestamp("created_at");
        r.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return r;
    }
}
