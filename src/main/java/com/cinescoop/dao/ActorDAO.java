package com.cinescoop.dao;

import com.cinescoop.database.DatabaseConnection;
import com.cinescoop.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ActorDAO — CRUD and batch operations for the `actors` table.
 */
public class ActorDAO {

    private static final Logger logger = LoggerFactory.getLogger(ActorDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    // ─── BATCH INSERT ────────────────────────────────────────────

    public int insertBatch(List<Actor> actors) {
        String sql = """
            INSERT INTO actors (actor_id, full_name, gender, birth_date, nationality,
                height, awards_count, debut_year, instagram_followers, salary,
                agent_name, email, phone, city, image_url, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), image_url = VALUES(image_url)
            """;
        int total = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (Actor a : actors) {
                ps.setInt(1, a.getActorId());
                ps.setString(2, a.getFullName());
                ps.setString(3, a.getGender());
                ps.setObject(4, a.getBirthDate(), Types.DATE);
                ps.setString(5, a.getNationality());
                ps.setObject(6, a.getHeight(), Types.FLOAT);
                ps.setInt(7, a.getAwardsCount());
                ps.setObject(8, a.getDebutYear(), Types.INTEGER);
                ps.setObject(9, a.getInstagramFollowers(), Types.BIGINT);
                ps.setObject(10, a.getSalary(), Types.DECIMAL);
                ps.setString(11, a.getAgentName());
                ps.setString(12, a.getEmail());
                ps.setString(13, a.getPhone());
                ps.setString(14, a.getCity());
                ps.setString(15, a.getImageUrl());
                ps.setObject(16, a.getCreatedAt(), Types.TIMESTAMP);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            conn.commit();
            for (int r : results) total += (r >= 0 ? r : 0);
            logger.info("✅ Batch inserted {} actors.", total);
        } catch (SQLException e) {
            logger.error("❌ Batch insert actors failed: {}", e.getMessage());
        }
        return total;
    }

    // ─── FIND ALL ────────────────────────────────────────────────

    public List<Actor> findAll() {
        List<Actor> list = new ArrayList<>();
        String sql = "SELECT * FROM actors ORDER BY actor_id";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("❌ findAll actors failed: {}", e.getMessage());
        }
        return list;
    }

    // ─── FIND BY ID ──────────────────────────────────────────────

    public Optional<Actor> findById(int id) {
        String sql = "SELECT * FROM actors WHERE actor_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            logger.error("❌ findById actor failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    // ─── COUNT ───────────────────────────────────────────────────

    public int count() {
        String sql = "SELECT COUNT(*) FROM actors";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("❌ count actors failed: {}", e.getMessage());
        }
        return 0;
    }

    // ─── DELETE ──────────────────────────────────────────────────

    public boolean deleteById(int id) {
        return db.executeUpdate("DELETE FROM actors WHERE actor_id = ?", id);
    }

    // ─── ROW MAPPER ──────────────────────────────────────────────

    private Actor mapRow(ResultSet rs) throws SQLException {
        Actor a = new Actor();
        a.setActorId(rs.getInt("actor_id"));
        a.setFullName(rs.getString("full_name"));
        a.setGender(rs.getString("gender"));
        Date bd = rs.getDate("birth_date");
        a.setBirthDate(bd != null ? bd.toLocalDate() : null);
        a.setNationality(rs.getString("nationality"));
        float h = rs.getFloat("height");
        a.setHeight(rs.wasNull() ? null : h);
        a.setAwardsCount(rs.getInt("awards_count"));
        int dy = rs.getInt("debut_year");
        a.setDebutYear(rs.wasNull() ? null : dy);
        long ig = rs.getLong("instagram_followers");
        a.setInstagramFollowers(rs.wasNull() ? null : ig);
        a.setSalary(rs.getBigDecimal("salary"));
        a.setAgentName(rs.getString("agent_name"));
        a.setEmail(rs.getString("email"));
        a.setPhone(rs.getString("phone"));
        a.setCity(rs.getString("city"));
        a.setImageUrl(rs.getString("image_url"));
        Timestamp ts = rs.getTimestamp("created_at");
        a.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return a;
    }
}
