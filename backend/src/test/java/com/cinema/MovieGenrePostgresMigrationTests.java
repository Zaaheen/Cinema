package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class MovieGenrePostgresMigrationTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_movie_genre_test")
            .withUsername("cinema")
            .withPassword("cinema");

    @Test
    void migratesFreshPostgresThroughV9AndRemainsIdempotent() throws Exception {
        Flyway flyway = flyway();

        assertEquals(9, flyway.migrate().migrationsExecuted);
        flyway.validate();
        assertEquals(0, flyway.migrate().migrationsExecuted);

        try (Connection connection = connect()) {
            assertEquals(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), successfulVersions(connection));
            assertEquals(6, count(connection, "select count(*) from genres where is_selectable"));
            assertEquals(6, count(connection, "select count(*) from genres"));
            assertTrue(constraintExists(connection, "ux_genres_name"));
            assertTrue(constraintExists(connection, "pk_movie_genres"));
            assertTrue(indexExists(connection, "ix_movie_genres_genre"));
        }
    }

    @Test
    void upgradesV1ThroughV6WithoutChangingLegacyMovieOrOperationalData() throws Exception {
        resetToVersionSix();
        try (Connection connection = connect()) {
            long canonicalMovie = insertMovie(connection, "Canonical", "Hành động", "canonical-key");
            long trimmedMovie = insertMovie(connection, "Trimmed", "  Kinh dị  ", "trimmed-key");
            long atomicMovie = insertMovie(connection, "Atomic", "Hành động, Hài kịch", "atomic-key");
            long lowerCaseMovie = insertMovie(connection, "Case", "hành động", "case-key");
            long accentMovie = insertMovie(connection, "Accent", "Hanh dong", "accent-key");
            long nullMovie = insertMovie(connection, "Null", null, "null-key");
            long blankMovie = insertMovie(connection, "Blank", "   ", "blank-key");
            insertRoomAndShowtime(connection, canonicalMovie);
            insertSeatPrice(connection);

            migrateToLatest();

            assertEquals("Hành động", legacyGenre(connection, canonicalMovie));
            assertEquals("  Kinh dị  ", legacyGenre(connection, trimmedMovie));
            assertEquals("Hành động, Hài kịch", legacyGenre(connection, atomicMovie));
            assertEquals("https://example.test/backdrop.jpg", movieColumn(connection, canonicalMovie, "backdrop_url"));
            assertEquals("T13", movieColumn(connection, canonicalMovie, "age_rating"));
            assertEquals("canonical-key", movieColumn(connection, canonicalMovie, "source_key"));

            assertGenre(connection, canonicalMovie, "Hành động", true);
            assertGenre(connection, trimmedMovie, "Kinh dị", true);
            assertGenre(connection, atomicMovie, "Hành động, Hài kịch", false);
            assertGenre(connection, lowerCaseMovie, "hành động", false);
            assertGenre(connection, accentMovie, "Hanh dong", false);
            assertEquals(0, mappingCount(connection, nullMovie));
            assertEquals(0, mappingCount(connection, blankMovie));
            assertEquals(1, count(connection, "select count(*) from genres where name = 'Hành động'"));
            assertEquals(1, count(connection, "select count(*) from genres where name = 'Kinh dị'"));
            assertEquals(1, count(connection, "select count(*) from genres where name = 'Hành động, Hài kịch'"));
            assertEquals(1, count(connection, "select count(*) from genres where name = 'hành động'"));
            assertEquals(1, count(connection, "select count(*) from genres where name = 'Hanh dong'"));
            assertEquals(1, count(connection, "select count(*) from showtimes"));
            assertEquals(1, count(connection, "select count(*) from seat_prices"));
        }
    }

    private Flyway flyway() {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load();
    }

    private void resetToVersionSix() {
        Flyway clean = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .cleanDisabled(false)
                .load();
        clean.clean();
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("6").load().migrate();
    }

    private void migrateToLatest() { flyway().migrate(); }

    private Connection connect() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private long insertMovie(Connection connection, String title, String genre, String sourceKey) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into movies (title, genre, duration_minutes, version, status, backdrop_url, age_rating, source_key)
                values (?, ?, 120, '2D', 'NOW_SHOWING', 'https://example.test/backdrop.jpg', 'T13', ?)
                returning id
                """)) {
            statement.setString(1, title);
            statement.setString(2, genre);
            statement.setString(3, sourceKey);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getLong(1);
            }
        }
    }

    private void insertRoomAndShowtime(Connection connection, long movieId) throws Exception {
        long roomId;
        try (PreparedStatement room = connection.prepareStatement(
                "insert into rooms (name) values ('V7 Test Room') returning id")) {
            try (ResultSet rows = room.executeQuery()) { rows.next(); roomId = rows.getLong(1); }
        }
        try (PreparedStatement showtime = connection.prepareStatement("""
                insert into showtimes (movie_id, room_id, starts_at, ends_at, status)
                values (?, ?, ?, ?, 'SCHEDULED')
                """)) {
            showtime.setLong(1, movieId);
            showtime.setLong(2, roomId);
            showtime.setTimestamp(3, Timestamp.from(java.time.Instant.parse("2027-01-01T10:00:00Z")));
            showtime.setTimestamp(4, Timestamp.from(java.time.Instant.parse("2027-01-01T12:00:00Z")));
            showtime.executeUpdate();
        }
    }

    private void insertSeatPrice(Connection connection) throws Exception {
        try (PreparedStatement price = connection.prepareStatement("""
                insert into seat_prices (seat_type, price, valid_from)
                values ('STANDARD', ?, ?)
                """)) {
            price.setBigDecimal(1, new BigDecimal("70000.00"));
            price.setObject(2, LocalDate.of(2027, 1, 1));
            price.executeUpdate();
        }
    }

    private void assertGenre(Connection connection, long movieId, String name, boolean selectable) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("""
                select g.name, g.is_selectable from movie_genres mg
                join genres g on g.id = mg.genre_id where mg.movie_id = ?
                """)) {
            statement.setLong(1, movieId);
            try (ResultSet rows = statement.executeQuery()) {
                assertTrue(rows.next());
                assertEquals(name, rows.getString("name"));
                assertEquals(selectable, rows.getBoolean("is_selectable"));
                assertFalse(rows.next());
            }
        }
    }

    private String legacyGenre(Connection connection, long movieId) throws Exception {
        return movieColumn(connection, movieId, "genre");
    }

    private String movieColumn(Connection connection, long movieId, String column) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select " + column + " from movies where id = ?")) {
            statement.setLong(1, movieId);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getString(1); }
        }
    }

    private long mappingCount(Connection connection, long movieId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select count(*) from movie_genres where movie_id = ?")) {
            statement.setLong(1, movieId);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    private long count(Connection connection, String query) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(query); ResultSet rows = statement.executeQuery()) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private Set<String> successfulVersions(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select version from flyway_schema_history where success = true and version is not null");
                ResultSet rows = statement.executeQuery()) {
            java.util.Set<String> versions = new java.util.HashSet<>();
            while (rows.next()) versions.add(rows.getString(1));
            return versions;
        }
    }

    private boolean constraintExists(Connection connection, String name) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select exists (select 1 from pg_constraint where conname = ?)")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getBoolean(1); }
        }
    }

    private boolean indexExists(Connection connection, String name) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("select to_regclass(?) is not null")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getBoolean(1); }
        }
    }
}
