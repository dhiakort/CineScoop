package com.cinescoop.dao;

import com.cinescoop.database.DatabaseConnection;
import com.cinescoop.model.MovieActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MovieActorDAO {

    private static final Logger logger = LoggerFactory.getLogger(MovieActorDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public int insertBatch(List<MovieActor> movieActors) {
        String sql = """
            INSERT INTO movie_actors (id, movie_id, actor_id, role_name, screen_time)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE role_name = VALUES(role_name)
            """;
        int total = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (MovieActor ma : movieActors) {
                ps.setInt(1, ma.getId());
                ps.setInt(2, ma.getMovieId());
                ps.setInt(3, ma.getActorId());
                ps.setString(4, ma.getRoleName());
                ps.setObject(5, ma.getScreenTime(), Types.INTEGER);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            conn.commit();
            for (int r : results) total += (r >= 0 ? r : 0);
            logger.info("Batch inserted {} movie_actors.", total);
        } catch (SQLException e) {
            logger.error("Batch insert movie_actors failed: {}", e.getMessage());
        }
        return total;
    }

    public List<MovieActor> findByMovieId(int movieId) {
        List<MovieActor> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM movie_actors WHERE movie_id = ?")) {
            ps.setInt(1, movieId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("findByMovieId movie_actors failed: {}", e.getMessage());
        }
        return list;
    }

    public int count() {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM movie_actors")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("count movie_actors failed: {}", e.getMessage());
        }
        return 0;
    }

    private MovieActor mapRow(ResultSet rs) throws SQLException {
        MovieActor ma = new MovieActor();
        ma.setId(rs.getInt("id"));
        ma.setMovieId(rs.getInt("movie_id"));
        ma.setActorId(rs.getInt("actor_id"));
        ma.setRoleName(rs.getString("role_name"));
        int st = rs.getInt("screen_time");
        ma.setScreenTime(rs.wasNull() ? null : st);
        return ma;
    }
}
