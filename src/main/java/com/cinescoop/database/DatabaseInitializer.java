package com.cinescoop.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseInitializer — creates the CineScoop schema on first run.
 *
 * Call DatabaseInitializer.initialize() once in MainApp before anything else.
 */
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    // Ordered DDL — foreign keys require parents to exist first
    private static final String[] DDL_STATEMENTS = {

            // 1. DIRECTORS
            """
        CREATE TABLE IF NOT EXISTS directors (
            director_id      INT AUTO_INCREMENT PRIMARY KEY,
            full_name        VARCHAR(100) NOT NULL,
            nationality      VARCHAR(50),
            experience_years INT,
            famous_movie     VARCHAR(150),
            awards           INT DEFAULT 0,
            email            VARCHAR(100) UNIQUE,
            phone            VARCHAR(20),
            salary           DECIMAL(15,2),
            studio           VARCHAR(100),
            gender           ENUM('M','F','Other'),
            birth_date       DATE,
            net_worth        DECIMAL(18,2),
            status           ENUM('Active','Retired') DEFAULT 'Active',
            created_at       DATETIME DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """,

            // 2. MOVIES
            """
        CREATE TABLE IF NOT EXISTS movies (
            movie_id           INT AUTO_INCREMENT PRIMARY KEY,
            title              VARCHAR(200) NOT NULL,
            genre              VARCHAR(50)  NOT NULL,
            release_year       YEAR,
            duration           INT,
            language           VARCHAR(30),
            country            VARCHAR(50),
            budget             DECIMAL(15,2),
            revenue            DECIMAL(15,2),
            rating_average     FLOAT,
            director_id        INT,
            production_company VARCHAR(100),
            age_limit          INT,
            status             ENUM('Released','Production','Cancelled') DEFAULT 'Released',
            created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_movie_director
                FOREIGN KEY (director_id) REFERENCES directors(director_id)
                ON DELETE SET NULL ON UPDATE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """,

            // 3. ACTORS
            """
        CREATE TABLE IF NOT EXISTS actors (
            actor_id            INT AUTO_INCREMENT PRIMARY KEY,
            full_name           VARCHAR(100) NOT NULL,
            gender              ENUM('M','F','Other'),
            birth_date          DATE,
            nationality         VARCHAR(50),
            height              FLOAT,
            awards_count        INT DEFAULT 0,
            debut_year          YEAR,
            instagram_followers BIGINT,
            salary              DECIMAL(15,2),
            agent_name          VARCHAR(100),
            email               VARCHAR(100) UNIQUE,
            phone               VARCHAR(20),
            city                VARCHAR(80),
            created_at          DATETIME DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """,

            // 4. MOVIE_ACTORS (join table)
            """
        CREATE TABLE IF NOT EXISTS movie_actors (
            id          INT AUTO_INCREMENT PRIMARY KEY,
            movie_id    INT NOT NULL,
            actor_id    INT NOT NULL,
            role_name   VARCHAR(100),
            screen_time INT,
            CONSTRAINT fk_ma_movie FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
            CONSTRAINT fk_ma_actor FOREIGN KEY (actor_id) REFERENCES actors(actor_id) ON DELETE CASCADE,
            UNIQUE KEY uq_movie_actor (movie_id, actor_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """,

            // 5. USERS
            """
        CREATE TABLE IF NOT EXISTS users (
            user_id           INT AUTO_INCREMENT PRIMARY KEY,
            username          VARCHAR(50)  NOT NULL UNIQUE,
            email             VARCHAR(100) NOT NULL UNIQUE,
            password          VARCHAR(255) NOT NULL,
            country           VARCHAR(50),
            age               INT,
            gender            ENUM('M','F','Other'),
            subscription_type ENUM('Free','Basic','Premium') DEFAULT 'Free',
            watch_time        INT DEFAULT 0,
            favorite_genre    VARCHAR(50),
            registration_date DATETIME DEFAULT CURRENT_TIMESTAMP,
            last_login        DATETIME,
            phone             VARCHAR(20),
            status            ENUM('Active','Inactive','Banned') DEFAULT 'Active',
            profile_picture   VARCHAR(255)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """,

            // 6. RATINGS
            """
        CREATE TABLE IF NOT EXISTS ratings (
            rating_id       INT AUTO_INCREMENT PRIMARY KEY,
            movie_id        INT NOT NULL,
            user_id         INT NOT NULL,
            rating_value    FLOAT NOT NULL,
            review          TEXT,
            review_date     DATE,
            likes           INT DEFAULT 0,
            dislikes        INT DEFAULT 0,
            watch_completed BOOLEAN DEFAULT FALSE,
            device_used     VARCHAR(50),
            watch_duration  INT,
            favorite_scene  VARCHAR(200),
            recommendation  BOOLEAN DEFAULT TRUE,
            created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT fk_rating_movie FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
            CONSTRAINT fk_rating_user  FOREIGN KEY (user_id)  REFERENCES users(user_id)  ON DELETE CASCADE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """,

            // 7. INDEXES for query performance
            "CREATE INDEX IF NOT EXISTS idx_movies_genre        ON movies(genre)",
            "CREATE INDEX IF NOT EXISTS idx_movies_year         ON movies(release_year)",
            "CREATE INDEX IF NOT EXISTS idx_movies_director     ON movies(director_id)",
            "CREATE INDEX IF NOT EXISTS idx_ratings_movie       ON ratings(movie_id)",
            "CREATE INDEX IF NOT EXISTS idx_ratings_user        ON ratings(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_ratings_value       ON ratings(rating_value)",
            "CREATE INDEX IF NOT EXISTS idx_users_subscription  ON users(subscription_type)",
            "CREATE INDEX IF NOT EXISTS idx_users_country       ON users(country)"
    };

    // ─────────────────────────────────────────────────────────────

    public static void initialize() {
        logger.info("🏗️  Initialising CineScoop database schema...");
        DatabaseConnection db = DatabaseConnection.getInstance();

        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {

            // Disable FK checks during creation to avoid ordering issues
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            int created = 0;
            for (String ddl : DDL_STATEMENTS) {
                try {
                    stmt.execute(ddl.strip());
                    created++;
                } catch (SQLException e) {
                    logger.warn("⚠️  DDL skipped or failed: {}", e.getMessage());
                }
            }

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            logger.info("✅ Schema ready — {} statements executed.", created);

        } catch (SQLException e) {
            logger.error("❌ Schema initialisation failed: {}", e.getMessage());
            throw new RuntimeException("Could not initialise database schema", e);
        }
    }
}