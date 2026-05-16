package com.cinescoop.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rating — POJO mapped to the `ratings` table.
 */
public class Rating {

    private int ratingId;
    private int movieId;
    private int userId;
    private float ratingValue;
    private String review;
    private LocalDate reviewDate;
    private int likes;
    private int dislikes;
    private boolean watchCompleted;
    private String deviceUsed;
    private Integer watchDuration;
    private String favoriteScene;
    private boolean recommendation;
    private LocalDateTime createdAt;

    // ─── Constructors ────────────────────────────────────────────

    public Rating() {}

    public Rating(int ratingId, int movieId, int userId, float ratingValue,
                  String review, LocalDate reviewDate, int likes, int dislikes,
                  boolean watchCompleted, String deviceUsed, Integer watchDuration,
                  String favoriteScene, boolean recommendation, LocalDateTime createdAt) {
        this.ratingId = ratingId;
        this.movieId = movieId;
        this.userId = userId;
        this.ratingValue = ratingValue;
        this.review = review;
        this.reviewDate = reviewDate;
        this.likes = likes;
        this.dislikes = dislikes;
        this.watchCompleted = watchCompleted;
        this.deviceUsed = deviceUsed;
        this.watchDuration = watchDuration;
        this.favoriteScene = favoriteScene;
        this.recommendation = recommendation;
        this.createdAt = createdAt;
    }

    // ─── Getters & Setters ───────────────────────────────────────

    public int getRatingId() { return ratingId; }
    public void setRatingId(int ratingId) { this.ratingId = ratingId; }

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public float getRatingValue() { return ratingValue; }
    public void setRatingValue(float ratingValue) { this.ratingValue = ratingValue; }

    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }

    public LocalDate getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDate reviewDate) { this.reviewDate = reviewDate; }

    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }

    public int getDislikes() { return dislikes; }
    public void setDislikes(int dislikes) { this.dislikes = dislikes; }

    public boolean isWatchCompleted() { return watchCompleted; }
    public void setWatchCompleted(boolean watchCompleted) { this.watchCompleted = watchCompleted; }

    public String getDeviceUsed() { return deviceUsed; }
    public void setDeviceUsed(String deviceUsed) { this.deviceUsed = deviceUsed; }

    public Integer getWatchDuration() { return watchDuration; }
    public void setWatchDuration(Integer watchDuration) { this.watchDuration = watchDuration; }

    public String getFavoriteScene() { return favoriteScene; }
    public void setFavoriteScene(String favoriteScene) { this.favoriteScene = favoriteScene; }

    public boolean isRecommendation() { return recommendation; }
    public void setRecommendation(boolean recommendation) { this.recommendation = recommendation; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Rating{" +
                "ratingId=" + ratingId +
                ", movieId=" + movieId +
                ", userId=" + userId +
                ", ratingValue=" + ratingValue +
                '}';
    }
}
