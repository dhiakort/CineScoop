package com.cinescoop.service;

import com.cinescoop.database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * StatsService — runs analytics queries for the JavaFX dashboard charts.
 */
public class StatsService {

    private static final Logger logger = LoggerFactory.getLogger(StatsService.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    /** Movies count per genre (for PieChart / BarChart). */
    public Map<String, Integer> moviesPerGenre() {
        return queryMap("SELECT genre, COUNT(*) AS cnt FROM movies GROUP BY genre ORDER BY cnt DESC");
    }

    /** Movies count per release year (for LineChart). */
    public Map<String, Integer> moviesPerYear() {
        return queryMap("SELECT CAST(release_year AS CHAR) AS yr, COUNT(*) AS cnt FROM movies GROUP BY release_year ORDER BY release_year");
    }

    /** Average rating per genre. */
    public Map<String, Double> avgRatingPerGenre() {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = """
            SELECT m.genre, ROUND(AVG(r.rating_value), 2) AS avg_rat
            FROM ratings r JOIN movies m ON r.movie_id = m.movie_id
            GROUP BY m.genre ORDER BY avg_rat DESC
            """;
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString(1), rs.getDouble(2));
        } catch (SQLException e) {
            logger.error("avgRatingPerGenre failed: {}", e.getMessage());
        }
        return map;
    }

    /** Users count per subscription type. */
    public Map<String, Integer> usersPerSubscription() {
        return queryMap("SELECT subscription_type, COUNT(*) AS cnt FROM users GROUP BY subscription_type ORDER BY cnt DESC");
    }

    /** Users count per country (top 10). */
    public Map<String, Integer> usersPerCountry() {
        return queryMap("SELECT country, COUNT(*) AS cnt FROM users GROUP BY country ORDER BY cnt DESC LIMIT 10");
    }

    /** Top 10 highest-revenue movies. */
    public Map<String, Double> topRevenueMovies() {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT title, revenue FROM movies WHERE revenue IS NOT NULL ORDER BY revenue DESC LIMIT 10";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString(1), rs.getDouble(2));
        } catch (SQLException e) {
            logger.error("topRevenueMovies failed: {}", e.getMessage());
        }
        return map;
    }

    /** Total counts for KPI cards. */
    public int totalMovies()    { return countTable("movies"); }
    public int totalActors()    { return countTable("actors"); }
    public int totalDirectors() { return countTable("directors"); }
    public int totalUsers()     { return countTable("users"); }
    public int totalRatings()   { return countTable("ratings"); }

    /** Average rating across all ratings. */
    public double overallAvgRating() {
        String sql = "SELECT ROUND(AVG(rating_value), 2) FROM ratings";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            logger.error("overallAvgRating failed: {}", e.getMessage());
        }
        return 0.0;
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private Map<String, Integer> queryMap(String sql) {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString(1), rs.getInt(2));
        } catch (SQLException e) {
            logger.error("queryMap failed: {}", e.getMessage());
        }
        return map;
    }

    private int countTable(String table) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("countTable({}) failed: {}", table, e.getMessage());
        }
        return 0;
    }
}
