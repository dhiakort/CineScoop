package com.cinescoop.dao;

import com.cinescoop.database.DatabaseConnection;
import com.cinescoop.model.Director;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DirectorDAO — CRUD and batch operations for the `directors` table.
 */
public class DirectorDAO {

    private static final Logger logger = LoggerFactory.getLogger(DirectorDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    // ─── INSERT ──────────────────────────────────────────────────

    public boolean insert(Director d) {
        String sql = """
            INSERT INTO directors (director_id, full_name, nationality, experience_years,
                famous_movie, awards, email, phone, salary, studio, gender, birth_date,
                net_worth, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE full_name = VALUES(full_name)
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, d.getDirectorId());
            ps.setString(2, d.getFullName());
            ps.setString(3, d.getNationality());
            ps.setObject(4, d.getExperienceYears(), Types.INTEGER);
            ps.setString(5, d.getFamousMovie());
            ps.setInt(6, d.getAwards());
            ps.setString(7, d.getEmail());
            ps.setString(8, d.getPhone());
            ps.setObject(9, d.getSalary(), Types.DECIMAL);
            ps.setString(10, d.getStudio());
            ps.setString(11, d.getGender());
            ps.setObject(12, d.getBirthDate(), Types.DATE);
            ps.setObject(13, d.getNetWorth(), Types.DECIMAL);
            ps.setString(14, d.getStatus());
            ps.setObject(15, d.getCreatedAt(), Types.TIMESTAMP);

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("❌ Insert director failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── BATCH INSERT ────────────────────────────────────────────

    public int insertBatch(List<Director> directors) {
        String sql = """
            INSERT INTO directors (director_id, full_name, nationality, experience_years,
                famous_movie, awards, email, phone, salary, studio, gender, birth_date,
                net_worth, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE full_name = VALUES(full_name)
            """;
        int total = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (Director d : directors) {
                ps.setInt(1, d.getDirectorId());
                ps.setString(2, d.getFullName());
                ps.setString(3, d.getNationality());
                ps.setObject(4, d.getExperienceYears(), Types.INTEGER);
                ps.setString(5, d.getFamousMovie());
                ps.setInt(6, d.getAwards());
                ps.setString(7, d.getEmail());
                ps.setString(8, d.getPhone());
                ps.setObject(9, d.getSalary(), Types.DECIMAL);
                ps.setString(10, d.getStudio());
                ps.setString(11, d.getGender());
                ps.setObject(12, d.getBirthDate(), Types.DATE);
                ps.setObject(13, d.getNetWorth(), Types.DECIMAL);
                ps.setString(14, d.getStatus());
                ps.setObject(15, d.getCreatedAt(), Types.TIMESTAMP);
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            conn.commit();
            for (int r : results) total += (r >= 0 ? r : 0);
            logger.info("✅ Batch inserted {} directors.", total);
        } catch (SQLException e) {
            logger.error("❌ Batch insert directors failed: {}", e.getMessage());
        }
        return total;
    }

    // ─── FIND BY ID ──────────────────────────────────────────────

    public Optional<Director> findById(int id) {
        String sql = "SELECT * FROM directors WHERE director_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            logger.error("❌ findById director failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    // ─── FIND ALL ────────────────────────────────────────────────

    public List<Director> findAll() {
        List<Director> list = new ArrayList<>();
        String sql = "SELECT * FROM directors ORDER BY director_id";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("❌ findAll directors failed: {}", e.getMessage());
        }
        return list;
    }

    // ─── COUNT ───────────────────────────────────────────────────

    public int count() {
        String sql = "SELECT COUNT(*) FROM directors";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("❌ count directors failed: {}", e.getMessage());
        }
        return 0;
    }

    // ─── DELETE ──────────────────────────────────────────────────

    public boolean deleteById(int id) {
        return db.executeUpdate("DELETE FROM directors WHERE director_id = ?", id);
    }

    // ─── ROW MAPPER ──────────────────────────────────────────────

    private Director mapRow(ResultSet rs) throws SQLException {
        Director d = new Director();
        d.setDirectorId(rs.getInt("director_id"));
        d.setFullName(rs.getString("full_name"));
        d.setNationality(rs.getString("nationality"));
        d.setExperienceYears(rs.getObject("experience_years", Integer.class));
        d.setFamousMovie(rs.getString("famous_movie"));
        d.setAwards(rs.getInt("awards"));
        d.setEmail(rs.getString("email"));
        d.setPhone(rs.getString("phone"));
        d.setSalary(rs.getBigDecimal("salary"));
        d.setStudio(rs.getString("studio"));
        d.setGender(rs.getString("gender"));
        Date bd = rs.getDate("birth_date");
        d.setBirthDate(bd != null ? bd.toLocalDate() : null);
        d.setNetWorth(rs.getBigDecimal("net_worth"));
        d.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        d.setCreatedAt(ts != null ? ts.toLocalDateTime() : null);
        return d;
    }
}
