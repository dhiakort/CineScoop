package com.cinescoop.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Actor — POJO mapped to the `actors` table.
 */
public class Actor {

    private int actorId;
    private String fullName;
    private String gender;
    private LocalDate birthDate;
    private String nationality;
    private Float height;
    private int awardsCount;
    private Integer debutYear;
    private Long instagramFollowers;
    private BigDecimal salary;
    private String agentName;
    private String email;
    private String phone;
    private String city;
    private String imageUrl;
    private LocalDateTime createdAt;

    // ─── Constructors ────────────────────────────────────────────

    public Actor() {}

    public Actor(int actorId, String fullName, String gender, LocalDate birthDate,
                 String nationality, Float height, int awardsCount, Integer debutYear,
                 Long instagramFollowers, BigDecimal salary, String agentName,
                 String email, String phone, String city, String imageUrl, LocalDateTime createdAt) {
        this.actorId = actorId;
        this.fullName = fullName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.nationality = nationality;
        this.height = height;
        this.awardsCount = awardsCount;
        this.debutYear = debutYear;
        this.instagramFollowers = instagramFollowers;
        this.salary = salary;
        this.agentName = agentName;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }

    // ─── Getters & Setters ───────────────────────────────────────

    public int getActorId() { return actorId; }
    public void setActorId(int actorId) { this.actorId = actorId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public Float getHeight() { return height; }
    public void setHeight(Float height) { this.height = height; }

    public int getAwardsCount() { return awardsCount; }
    public void setAwardsCount(int awardsCount) { this.awardsCount = awardsCount; }

    public Integer getDebutYear() { return debutYear; }
    public void setDebutYear(Integer debutYear) { this.debutYear = debutYear; }

    public Long getInstagramFollowers() { return instagramFollowers; }
    public void setInstagramFollowers(Long instagramFollowers) { this.instagramFollowers = instagramFollowers; }

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Actor{" +
                "actorId=" + actorId +
                ", fullName='" + fullName + '\'' +
                ", nationality='" + nationality + '\'' +
                ", awardsCount=" + awardsCount +
                '}';
    }
}
