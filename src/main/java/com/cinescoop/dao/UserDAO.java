package com.cinescoop.dao;

import com.cinescoop.database.DatabaseConnection;
import com.cinescoop.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    private final DatabaseConnection db = DatabaseConnection.getInstance();

    public int insertBatch(List<User> users) {
        String sql = """
            INSERT INTO users (user_id, username, email, password, country, age, gender,
                subscription_type, watch_time, favorite_genre, registration_date,
                last_login, phone, status, profile_picture)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE username = VALUES(username)
            """;
        int total = 0;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            for (User u : users) {
                ps.setInt(1, u.getUserId());
                ps.setString(2, u.getUsername());
                ps.setString(3, u.getEmail());
                ps.setString(4, u.getPassword());
                ps.setString(5, u.getCountry());
                ps.setObject(6, u.getAge(), Types.INTEGER);
                ps.setString(7, u.getGender());
                ps.setString(8, u.getSubscriptionType());
                ps.setInt(9, u.getWatchTime());
                ps.setString(10, u.getFavoriteGenre());
                ps.setObject(11, u.getRegistrationDate(), Types.TIMESTAMP);
                ps.setObject(12, u.getLastLogin(), Types.TIMESTAMP);
                ps.setString(13, u.getPhone());
                ps.setString(14, u.getStatus());
                ps.setString(15, u.getProfilePicture());
                ps.addBatch();
            }
            int[] results = ps.executeBatch();
            conn.commit();
            for (int r : results) total += (r >= 0 ? r : 0);
            logger.info("Batch inserted {} users.", total);
        } catch (SQLException e) {
            logger.error("Batch insert users failed: {}", e.getMessage());
        }
        return total;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users ORDER BY user_id")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("findAll users failed: {}", e.getMessage());
        }
        return list;
    }

    public Optional<User> findById(int id) {
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE user_id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            logger.error("findById user failed: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public int count() {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("count users failed: {}", e.getMessage());
        }
        return 0;
    }

    public boolean deleteById(int id) {
        return db.executeUpdate("DELETE FROM users WHERE user_id = ?", id);
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setCountry(rs.getString("country"));
        int age = rs.getInt("age"); u.setAge(rs.wasNull() ? null : age);
        u.setGender(rs.getString("gender"));
        u.setSubscriptionType(rs.getString("subscription_type"));
        u.setWatchTime(rs.getInt("watch_time"));
        u.setFavoriteGenre(rs.getString("favorite_genre"));
        Timestamp reg = rs.getTimestamp("registration_date");
        u.setRegistrationDate(reg != null ? reg.toLocalDateTime() : null);
        Timestamp ll = rs.getTimestamp("last_login");
        u.setLastLogin(ll != null ? ll.toLocalDateTime() : null);
        u.setPhone(rs.getString("phone"));
        u.setStatus(rs.getString("status"));
        u.setProfilePicture(rs.getString("profile_picture"));
        return u;
    }
}
