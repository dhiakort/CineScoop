package com.cinescoop.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Director — POJO mapped to the `directors` table.
 */
public class Director {

    private int directorId;
    private String fullName;
    private String nationality;
    private Integer experienceYears;
    private String famousMovie;
    private int awards;
    private String email;
    private String phone;
    private BigDecimal salary;
    private String studio;
    private String gender;
    private LocalDate birthDate;
    private BigDecimal netWorth;
    private String status;
    private LocalDateTime createdAt;

    // ─── Constructors ────────────────────────────────────────────

    public Director() {}

    public Director(int directorId, String fullName, String nationality,
                    Integer experienceYears, String famousMovie, int awards,
                    String email, String phone, BigDecimal salary, String studio,
                    String gender, LocalDate birthDate, BigDecimal netWorth,
                    String status, LocalDateTime createdAt) {
        this.directorId = directorId;
        this.fullName = fullName;
        this.nationality = nationality;
        this.experienceYears = experienceYears;
        this.famousMovie = famousMovie;
        this.awards = awards;
        this.email = email;
        this.phone = phone;
        this.salary = salary;
        this.studio = studio;
        this.gender = gender;
        this.birthDate = birthDate;
        this.netWorth = netWorth;
        this.status = status;
        this.createdAt = createdAt;
    }

    // ─── Getters & Setters ───────────────────────────────────────

    public int getDirectorId() { return directorId; }
    public void setDirectorId(int directorId) { this.directorId = directorId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }

    public String getFamousMovie() { return famousMovie; }
    public void setFamousMovie(String famousMovie) { this.famousMovie = famousMovie; }

    public int getAwards() { return awards; }
    public void setAwards(int awards) { this.awards = awards; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public String getStudio() { return studio; }
    public void setStudio(String studio) { this.studio = studio; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public BigDecimal getNetWorth() { return netWorth; }
    public void setNetWorth(BigDecimal netWorth) { this.netWorth = netWorth; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Director{" +
                "directorId=" + directorId +
                ", fullName='" + fullName + '\'' +
                ", nationality='" + nationality + '\'' +
                ", awards=" + awards +
                ", studio='" + studio + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
