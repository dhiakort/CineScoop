package com.cinescoop.dao;

import com.cinescoop.database.DatabaseConnection;
import com.cinescoop.model.Movie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * MovieDAO — CRUD and batch operations for the `movies` table.
 */
public class MovieDAO {

    private static final Logger logger = LoggerFactory.getLogger(MovieDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    // ─── INSERT ──────────────────────────────────────────────────

    public boolean insert(Movie m) {
        String sql = """
            INSERT INTO movies (movie_id, title, genre, release_year, duration, language,
                country, budget, revenue, rating_average, director_id, production_company,
                age_limit, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE title = VALUES(title)
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            setMovieParams(ps, m);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("❌ Insert movie failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── BATCH INSERT ────────────────────────────────────────────

    public int insertBatch(List<Movie> movies) {
        String sql = """
            INSERT INTO movies (movie_id, title, genre, release_year, duration, language,
                country, budget, revenue, rating_average, director_id, production_company,
                age_limit, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE title = VALUES(title)
            """;
        int total = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (Movie m : movies) {
                setMovieParams(ps, m);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            conn.commit();
            for (int r : results) total += (r >= 0 ? r : 0);
            logger.info("✅ Batch inserted {} movies.", total);
        } catch (SQLException e) {
            logger.error("❌ Batch insert movies failed: {}", e.getMessage());
        }
        return total;
    }

    // ─── FIND ALL ────────────────────────────────────────────────

    public List<Movie> findAll() {
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT * FROM movies ORDER BY movie_id";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("❌ findAll movies failed: {}", e.getMessage());
        }
        return list;
    }

    // ─── FIND BY ID ──────────────────────────────────────────────

    public Optional<Movie> findById(int id) {
        String sql = "SELECT * FROM movies WHERE movie_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            logger.error("❌ findById movie failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    // ─── FIND BY GENRE ───────────────────────────────────────────

    public List<Movie> findByGenre(String genre) {
        List<Movie> list = new ArrayList<>();
        String sql = "SELECT * FROM movies WHERE genre = ? ORDER BY release_year DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, genre);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("❌ findByGenre movies failed: {}", e.getMessage());
        }
        return list;
    }

    // ─── COUNT ───────────────────────────────────────────────────

    public int count() {
        String sql = "SELECT COUNT(*) FROM movies";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("❌ count movies failed: {}", e.getMessage());
        }
        return 0;
    }

    // ─── DELETE ──────────────────────────────────────────────────

    public boolean deleteById(int id) {
        return db.executeUpdate("DELETE FROM movies WHERE movie_id = ?", id);
    }

    // ─── HELPERS ─────────────────────────────────────────────────

    private void setMovieParams(PreparedStatement ps, Movie m) throws SQLException {
        ps.setInt(1, m.getMovieId());
        ps.setString(2, m.getTitle());
        ps.setString(3, m.getGenre());
        ps.setObject(4, m.getReleaseYear(), Types.INTEGER);
        ps.setObject(5, m.getDuration(), Types.INTEGER);
        ps.setString(6, m.getLanguage());
        ps.setString(7, m.getCountry());
        ps.setObject(8, m.getBudget(), Types.DECIMAL);
        ps.setObject(9, m.getRevenue(), Types.DECIMAL);
        ps.setObject(10, m.getRatingAverage(), Types.FLOAT);
        ps.setObject(11, m.getDirectorId(), Types.INTEGER);
        ps.setString(12, m.getProductionCompany());
        ps.setObject(13, m.getAgeLimit(), Types.INTEGER);
        ps.setString(14, m.getStatus());
        ps.setObject(15, m.getCreatedAt(), Types.TIMESTAMP);
    }

    private Movie mapRow(ResultSet rs) throws SQLException {
        Movie m = new Movie();
        m.setMovieId(rs.getInt("movie_id"));
        m.setTitle(rs.getString("title"));
        m.setGenre(rs.getString("genre"));
        m.setReleaseYear(rs.getObject("release_year", Integer.class));
        m.setDuration(rs.getObject("duration", Integer.class));
        m.setLanguage(rs.getString("language"));
        m.setCountry(rs.getString("country"));
        m.setBudget(rs.getBigDecimal("budget"));
        m.setRevenue(rs.getBigDecimal("revenue"));
        float rating = rs.getFloat("rating_average");
        m.setRatingAverage(rs.wasNull() ? null : rating);
        int dirId = rs.getInt("director_id");
        m.setDirectorId(rs.wasNull() ? null : dirId);
        m.setProductionCompany(rs.getString("production_company"));
        int age = rs.getInt("age_limit");
        m.setAgeLimit(rs.wasNull() ? null : age);
        m.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        m.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return m;
    }
}
