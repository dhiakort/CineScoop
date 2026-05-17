package com.cinescoop.service;

import com.cinescoop.dao.*;
import com.cinescoop.model.*;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.sql.*;

/**
 * CsvImportService — reads CSV files, cleans data, and inserts into MySQL.
 *
 * Import order respects FK constraints:
 *   1. directors  2. movies  3. actors  4. users  5. movie_actors  6. ratings
 */
public class CsvImportService {

    private static final Logger logger = LoggerFactory.getLogger(CsvImportService.class);
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DirectorDAO   directorDAO   = new DirectorDAO();
    private final MovieDAO      movieDAO      = new MovieDAO();
    private final ActorDAO      actorDAO      = new ActorDAO();
    private final UserDAO       userDAO       = new UserDAO();
    private final RatingDAO     ratingDAO     = new RatingDAO();
    private final MovieActorDAO movieActorDAO = new MovieActorDAO();

    private int cleanedRows = 0;
    private int skippedRows = 0;

    /**
     * Step A: Import parent tables that have no FK dependencies.
     */
    public void importParents(String baseDir) {
        logger.info("=== CSV IMPORT: PARENTS START ===");
        cleanedRows = 0;
        skippedRows = 0;

        importDirectors(baseDir + "/directors.csv");
        importActors(baseDir + "/actors.csv");
        importUsers(baseDir + "/users.csv");
        
        logger.info("=== CSV PARENTS DONE — cleaned: {} | skipped: {} ===", cleanedRows, skippedRows);
    }

    /**
     * Step C: Import child tables that depend on Movies, Users, and Actors.
     */
    public void importChildren(String baseDir) {
        logger.info("=== CSV IMPORT: CHILDREN START ===");
        int startCleaned = cleanedRows;
        
        importMovieActors(baseDir + "/movie_actors.csv");
        importRatings(baseDir + "/ratings.csv");

        logger.info("=== CSV CHILDREN DONE — cleaned: {} ===", (cleanedRows - startCleaned));
    }

    private Set<Integer> fetchValidIds(String table, String column) {
        Set<Integer> ids = new HashSet<>();
        String sql = "SELECT " + column + " FROM " + table;
        try (Connection conn = com.cinescoop.database.DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            logger.error("Failed fetching IDs from {}: {}", table, e.getMessage());
        }
        return ids;
    }

    // ─── DIRECTORS ───────────────────────────────────────────────

    private void importDirectors(String path) {
        List<Director> list = new ArrayList<>();
        try (Reader reader = new FileReader(path);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] row;
            while ((row = csv.readNext()) != null) {
                try {
                    Director d = new Director();
                    d.setDirectorId(parseInt(row[0]));
                    d.setFullName(clean(row[1]));
                    if (d.getFullName().isEmpty()) { skippedRows++; continue; }
                    d.setNationality(clean(row[2]));
                    d.setExperienceYears(parseIntOrNull(row[3]));
                    d.setFamousMovie(clean(row[4]));
                    d.setAwards(parseIntSafe(row[5]));
                    d.setEmail(cleanEmail(row[6]));
                    d.setPhone(clean(row[7]));
                    d.setSalary(parseBigDecimal(row[8]));
                    d.setStudio(clean(row[9]));
                    d.setGender(cleanGender(row[10]));
                    d.setBirthDate(parseDate(row[11]));
                    d.setNetWorth(parseBigDecimal(row[12]));
                    d.setStatus(cleanStatus(row[13], "Active", "Retired"));
                    d.setCreatedAt(parseDateTime(row[14]));
                    list.add(d);
                    cleanedRows++;
                } catch (Exception e) {
                    skippedRows++;
                }
            }
        } catch (Exception e) {
            logger.error("Failed reading {}: {}", path, e.getMessage());
        }
        if (!list.isEmpty()) directorDAO.insertBatch(list);
        logger.info("Directors: {} imported, {} skipped", list.size(), skippedRows);
    }

    // ─── MOVIES ──────────────────────────────────────────────────

    private void importMovies(String path) {
        List<Movie> list = new ArrayList<>();
        int localSkip = 0;
        try (Reader reader = new FileReader(path);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] row;
            while ((row = csv.readNext()) != null) {
                try {
                    Movie m = new Movie();
                    m.setMovieId(parseInt(row[0]));
                    m.setTitle(clean(row[1]));
                    if (m.getTitle().isEmpty()) { localSkip++; skippedRows++; continue; }
                    m.setGenre(clean(row[2]));
                    m.setReleaseYear(parseIntOrNull(row[3]));
                    m.setDuration(parseIntOrNull(row[4]));
                    m.setLanguage(clean(row[5]));
                    m.setCountry(clean(row[6]));
                    m.setBudget(parseBigDecimal(row[7]));
                    m.setRevenue(parseBigDecimal(row[8]));
                    m.setRatingAverage(parseFloatOrNull(row[9]));
                    m.setDirectorId(parseIntOrNull(row[10]));
                    m.setProductionCompany(clean(row[11]));
                    m.setAgeLimit(parseIntOrNull(row[12]));
                    m.setStatus(cleanStatus(row[13], "Released", "Production", "Cancelled"));
                    m.setCreatedAt(parseDateTime(row[14]));
                    list.add(m);
                    cleanedRows++;
                } catch (Exception e) {
                    localSkip++; skippedRows++;
                }
            }
        } catch (Exception e) {
            logger.error("Failed reading {}: {}", path, e.getMessage());
        }
        if (!list.isEmpty()) movieDAO.insertBatch(list);
        logger.info("Movies: {} imported, {} skipped", list.size(), localSkip);
    }

    // ─── ACTORS ──────────────────────────────────────────────────

    private void importActors(String path) {
        List<Actor> list = new ArrayList<>();
        int localSkip = 0;
        try (Reader reader = new FileReader(path);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] row;
            while ((row = csv.readNext()) != null) {
                try {
                    Actor a = new Actor();
                    a.setActorId(parseInt(row[0]));
                    a.setFullName(clean(row[1]));
                    if (a.getFullName().isEmpty()) { localSkip++; skippedRows++; continue; }
                    a.setGender(cleanGender(row[2]));
                    a.setBirthDate(parseDate(row[3]));
                    a.setNationality(clean(row[4]));
                    a.setHeight(parseFloatOrNull(row[5]));
                    a.setAwardsCount(parseIntSafe(row[6]));
                    a.setDebutYear(parseIntOrNull(row[7]));
                    a.setInstagramFollowers(parseLongOrNull(row[8]));
                    a.setSalary(parseBigDecimal(row[9]));
                    a.setAgentName(clean(row[10]));
                    a.setEmail(cleanEmail(row[11]));
                    a.setPhone(clean(row[12]));
                    a.setCity(clean(row[13]));
                    a.setCreatedAt(parseDateTime(row[14]));
                    if (row.length > 15) {
                        a.setImageUrl(clean(row[15]));
                    } else {
                        // Safe fallback image
                        a.setImageUrl("https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&q=80&w=256&h=256");
                    }
                    list.add(a);
                    cleanedRows++;
                } catch (Exception e) {
                    localSkip++; skippedRows++;
                }
            }
        } catch (Exception e) {
            logger.error("Failed reading {}: {}", path, e.getMessage());
        }
        if (!list.isEmpty()) actorDAO.insertBatch(list);
        logger.info("Actors: {} imported, {} skipped", list.size(), localSkip);
    }

    // ─── USERS ───────────────────────────────────────────────────

    private void importUsers(String path) {
        List<User> list = new ArrayList<>();
        int localSkip = 0;
        try (Reader reader = new FileReader(path);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] row;
            while ((row = csv.readNext()) != null) {
                try {
                    User u = new User();
                    u.setUserId(parseInt(row[0]));
                    u.setUsername(clean(row[1]));
                    if (u.getUsername().isEmpty()) { localSkip++; skippedRows++; continue; }
                    u.setEmail(cleanEmail(row[2]));
                    u.setPassword(clean(row[3]));
                    u.setCountry(clean(row[4]));
                    u.setAge(parseIntOrNull(row[5]));
                    u.setGender(cleanGender(row[6]));
                    u.setSubscriptionType(cleanStatus(row[7], "Free", "Basic", "Premium"));
                    u.setWatchTime(parseIntSafe(row[8]));
                    u.setFavoriteGenre(clean(row[9]));
                    u.setRegistrationDate(parseDateTime(row[10]));
                    u.setLastLogin(parseDateTime(row[11]));
                    u.setPhone(clean(row[12]));
                    u.setStatus(cleanStatus(row[13], "Active", "Inactive", "Banned"));
                    u.setProfilePicture(clean(row[14]));
                    list.add(u);
                    cleanedRows++;
                } catch (Exception e) {
                    localSkip++; skippedRows++;
                }
            }
        } catch (Exception e) {
            logger.error("Failed reading {}: {}", path, e.getMessage());
        }
        if (!list.isEmpty()) userDAO.insertBatch(list);
        logger.info("Users: {} imported, {} skipped", list.size(), localSkip);
    }

    // ─── MOVIE_ACTORS ────────────────────────────────────────────

    private void importMovieActors(String path) {
        List<MovieActor> list = new ArrayList<>();
        int localSkip = 0;
        
        Set<Integer> validMovieIds = fetchValidIds("movies", "movie_id");
        Set<Integer> validActorIds = fetchValidIds("actors", "actor_id");

        try (Reader reader = new FileReader(path);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] row;
            while ((row = csv.readNext()) != null) {
                try {
                    int mid = parseInt(row[1]);
                    int aid = parseInt(row[2]);
                    
                    // Validation solution: skip if FK doesn't exist
                    if (!validMovieIds.contains(mid) || !validActorIds.contains(aid)) {
                        localSkip++; skippedRows++; continue;
                    }

                    MovieActor ma = new MovieActor();
                    ma.setId(parseInt(row[0]));
                    ma.setMovieId(mid);
                    ma.setActorId(aid);
                    ma.setRoleName(clean(row[3]));
                    ma.setScreenTime(parseIntOrNull(row[4]));
                    list.add(ma);
                    cleanedRows++;
                } catch (Exception e) {
                    localSkip++; skippedRows++;
                }
            }
        } catch (Exception e) {
            logger.error("Failed reading {}: {}", path, e.getMessage());
        }
        if (!list.isEmpty()) movieActorDAO.insertBatch(list);
        logger.info("MovieActors: {} imported, {} skipped due to missing references", list.size(), localSkip);
    }

    // ─── RATINGS ─────────────────────────────────────────────────

    private void importRatings(String path) {
        List<Rating> list = new ArrayList<>();
        int localSkip = 0;
        
        Set<Integer> validMovieIds = fetchValidIds("movies", "movie_id");
        Set<Integer> validUserIds = fetchValidIds("users", "user_id");

        try (Reader reader = new FileReader(path);
             CSVReader csv = new CSVReaderBuilder(reader).withSkipLines(1).build()) {

            String[] row;
            while ((row = csv.readNext()) != null) {
                try {
                    int mid = parseInt(row[1]);
                    int uid = parseInt(row[2]);
                    
                    // Validation solution: skip if FK doesn't exist
                    if (!validMovieIds.contains(mid) || !validUserIds.contains(uid)) {
                        localSkip++; skippedRows++; continue;
                    }

                    Rating r = new Rating();
                    r.setRatingId(parseInt(row[0]));
                    r.setMovieId(mid);
                    r.setUserId(uid);
                    r.setRatingValue(parseFloatSafe(row[3]));
                    r.setReview(clean(row[4]));
                    r.setReviewDate(parseDate(row[5]));
                    r.setLikes(parseIntSafe(row[6]));
                    r.setDislikes(parseIntSafe(row[7]));
                    r.setWatchCompleted(parseBool(row[8]));
                    r.setDeviceUsed(clean(row[9]));
                    r.setWatchDuration(parseIntOrNull(row[10]));
                    r.setFavoriteScene(clean(row[11]));
                    r.setRecommendation(parseBool(row[12]));
                    r.setCreatedAt(parseDateTime(row[13]));
                    list.add(r);
                    cleanedRows++;
                } catch (Exception e) {
                    localSkip++; skippedRows++;
                }
            }
        } catch (Exception e) {
            logger.error("Failed reading {}: {}", path, e.getMessage());
        }
        if (!list.isEmpty()) ratingDAO.insertBatch(list);
        logger.info("Ratings: {} imported, {} skipped due to missing references", list.size(), localSkip);
    }

    // ═══════════════════════════════════════════════════════════════
    //  DATA CLEANING UTILITIES
    // ═══════════════════════════════════════════════════════════════

    private String clean(String s) {
        if (s == null) return "";
        return s.strip().replaceAll("\\s+", " ");
    }

    private String cleanEmail(String s) {
        String e = clean(s).toLowerCase();
        return e.contains("@") ? e : "";
    }

    private String cleanGender(String s) {
        String g = clean(s).toUpperCase();
        return switch (g) {
            case "M", "MALE"   -> "M";
            case "F", "FEMALE" -> "F";
            default            -> "Other";
        };
    }

    private String cleanStatus(String s, String... allowed) {
        String cleaned = clean(s);
        for (String a : allowed) {
            if (a.equalsIgnoreCase(cleaned)) return a;
        }
        return allowed.length > 0 ? allowed[0] : cleaned;
    }

    private int parseInt(String s) {
        return Integer.parseInt(clean(s));
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(clean(s)); }
        catch (Exception e) { return 0; }
    }

    private Integer parseIntOrNull(String s) {
        try { return Integer.parseInt(clean(s)); }
        catch (Exception e) { return null; }
    }

    private Long parseLongOrNull(String s) {
        try { return Long.parseLong(clean(s)); }
        catch (Exception e) { return null; }
    }

    private Float parseFloatOrNull(String s) {
        try { return Float.parseFloat(clean(s)); }
        catch (Exception e) { return null; }
    }

    private float parseFloatSafe(String s) {
        try { return Float.parseFloat(clean(s)); }
        catch (Exception e) { return 0f; }
    }

    private BigDecimal parseBigDecimal(String s) {
        try { return new BigDecimal(clean(s)); }
        catch (Exception e) { return null; }
    }

    private LocalDate parseDate(String s) {
        try { return LocalDate.parse(clean(s), D_FMT); }
        catch (DateTimeParseException e) { return null; }
    }

    private LocalDateTime parseDateTime(String s) {
        try { return LocalDateTime.parse(clean(s), DT_FMT); }
        catch (DateTimeParseException e) { return null; }
    }

    private boolean parseBool(String s) {
        String v = clean(s).toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("yes");
    }

    // ─── Getters for stats ───────────────────────────────────────

    public int getCleanedRows() { return cleanedRows; }
    public int getSkippedRows() { return skippedRows; }
}
