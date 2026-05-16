package com.cinescoop.model;

/**
 * MovieActor — POJO mapped to the `movie_actors` join table.
 */
public class MovieActor {

    private int id;
    private int movieId;
    private int actorId;
    private String roleName;
    private Integer screenTime;

    // ─── Constructors ────────────────────────────────────────────

    public MovieActor() {}

    public MovieActor(int id, int movieId, int actorId, String roleName, Integer screenTime) {
        this.id = id;
        this.movieId = movieId;
        this.actorId = actorId;
        this.roleName = roleName;
        this.screenTime = screenTime;
    }

    // ─── Getters & Setters ───────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }

    public int getActorId() { return actorId; }
    public void setActorId(int actorId) { this.actorId = actorId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public Integer getScreenTime() { return screenTime; }
    public void setScreenTime(Integer screenTime) { this.screenTime = screenTime; }

    @Override
    public String toString() {
        return "MovieActor{" +
                "id=" + id +
                ", movieId=" + movieId +
                ", actorId=" + actorId +
                ", roleName='" + roleName + '\'' +
                '}';
    }
}
