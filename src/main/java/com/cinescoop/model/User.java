package com.cinescoop.model;

import java.time.LocalDateTime;

/**
 * User — POJO mapped to the `users` table.
 */
public class User {

    private int userId;
    private String username;
    private String email;
    private String password;
    private String country;
    private Integer age;
    private String gender;
    private String subscriptionType;
    private int watchTime;
    private String favoriteGenre;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLogin;
    private String phone;
    private String status;
    private String profilePicture;

    // ─── Constructors ────────────────────────────────────────────

    public User() {}

    public User(int userId, String username, String email, String password,
                String country, Integer age, String gender, String subscriptionType,
                int watchTime, String favoriteGenre, LocalDateTime registrationDate,
                LocalDateTime lastLogin, String phone, String status, String profilePicture) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.password = password;
        this.country = country;
        this.age = age;
        this.gender = gender;
        this.subscriptionType = subscriptionType;
        this.watchTime = watchTime;
        this.favoriteGenre = favoriteGenre;
        this.registrationDate = registrationDate;
        this.lastLogin = lastLogin;
        this.phone = phone;
        this.status = status;
        this.profilePicture = profilePicture;
    }

    // ─── Getters & Setters ───────────────────────────────────────

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSubscriptionType() { return subscriptionType; }
    public void setSubscriptionType(String subscriptionType) { this.subscriptionType = subscriptionType; }

    public int getWatchTime() { return watchTime; }
    public void setWatchTime(int watchTime) { this.watchTime = watchTime; }

    public String getFavoriteGenre() { return favoriteGenre; }
    public void setFavoriteGenre(String favoriteGenre) { this.favoriteGenre = favoriteGenre; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", subscriptionType='" + subscriptionType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
