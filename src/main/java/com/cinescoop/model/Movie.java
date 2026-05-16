package com.cinescoop.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Movie — POJO mapped to the `movies` table.
 */
public class Movie {

    private int movieId;
    private String title;
    private String genre;
    private Integer releaseYear;
    private Integer duration;
    private String language;
    private String country;
    private BigDecimal budget;
    private BigDecimal revenue;
    private Float ratingAverage;
    private Integer directorId;
    private String productionCompany;
    private Integer ageLimit;
    private String status;
    private LocalDateTime createdAt;

    // ─── Constructors ────────────────────────────────────────────

    public Movie() {}

    public Movie(int movieId, String title, String genre, Integer releaseYear,
                 Integer duration, String language, String country,
                 BigDecimal budget, BigDecimal revenue, Float ratingAverage,
                 Integer directorId, String productionCompany, Integer ageLimit,
                 String status, LocalDateTime createdAt) {
        this.movieId = movieId;
        this.title = title;
        this.genre = genre;
        this.releaseYear = releaseYear;
        this.duration = duration;
        this.language = language;
        this.country = country;
        this.budget = budget;
        this.revenue = revenue;
        this.ratingAverage = ratingAverage;
        this.directorId = directorId;
        this.productionCompany = productionCompany;
        this.ageLimit = ageLimit;
        this.status = status;
        this.createdAt = createdAt;
    }

    // ─── Getters & Setters ───────────────────────────────────────

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public Integer getReleaseYear() { return releaseYear; }
    public void setReleaseYear(Integer releaseYear) { this.releaseYear = releaseYear; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public Float getRatingAverage() { return ratingAverage; }
    public void setRatingAverage(Float ratingAverage) { this.ratingAverage = ratingAverage; }

    public Integer getDirectorId() { return directorId; }
    public void setDirectorId(Integer directorId) { this.directorId = directorId; }

    public String getProductionCompany() { return productionCompany; }
    public void setProductionCompany(String productionCompany) { this.productionCompany = productionCompany; }

    public Integer getAgeLimit() { return ageLimit; }
    public void setAgeLimit(Integer ageLimit) { this.ageLimit = ageLimit; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Movie{" +
                "movieId=" + movieId +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", releaseYear=" + releaseYear +
                ", ratingAverage=" + ratingAverage +
                ", status='" + status + '\'' +
                '}';
    }
}
