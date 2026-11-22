package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class SeatPricePostgresMigrationTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_test")
            .withUsername("cinema")
            .withPassword("cinema");

    @Test
    void migratesLegacyRowsAndEnforcesApprovedPriceConflictsOnPostgres() throws Exception {
        migrateToVersionFive();
        try (Connection connection = connect()) {
            insertLegacy(connection, "STANDARD", "2D   Phụ đề", new BigDecimal("90000"),
                    LocalDate.of(2026, 1, 1), null);
        }
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();

        try (Connection connection = connect()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "select status, origin, movie_version_key from seat_prices where id = 1")) {
                try (ResultSet rows = statement.executeQuery()) {
                    assertTrue(rows.next());
                    assertEquals("INACTIVE", rows.getString("status"));
                    assertEquals("LOCAL_FIXTURE", rows.getString("origin"));
                    assertEquals("2D PHỤ ĐỀ", rows.getString("movie_version_key"));
                }
            }

            insert(connection, "STANDARD", null, new BigDecimal("100000"),
                    LocalDate.of(2026, 12, 1), null, "ACTIVE", "ADMIN_APPROVED");
            assertThrows(SQLException.class, () -> insert(connection, "STANDARD", null,
                    new BigDecimal("110000"), LocalDate.of(2026, 12, 2), null,
                    "ACTIVE", "ADMIN_APPROVED"));

            insert(connection, "STANDARD", "IMAX   Laser", new BigDecimal("150000"),
                    LocalDate.of(2026, 12, 1), null, "ACTIVE", "ADMIN_APPROVED");
            assertThrows(SQLException.class, () -> insert(connection, "STANDARD", "imax laser",
                    new BigDecimal("160000"), LocalDate.of(2026, 12, 2), null,
                    "ACTIVE", "ADMIN_APPROVED"));
        }
    }

    @Test
    void concurrentApprovedGenericRulesAllowOnlyOneCommit() throws Exception {
        migrateToVersionFive();
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> attempt = () -> {
                ready.countDown();
                start.await();
                try (Connection connection = connect()) {
                    connection.setAutoCommit(false);
                    try {
                        insert(connection, "VIP", null, new BigDecimal("120000"),
                                LocalDate.of(2026, 12, 1), null, "ACTIVE", "ADMIN_APPROVED");
                        connection.commit();
                        return true;
                    } catch (SQLException exception) {
                        connection.rollback();
                        return false;
                    }
                }
            };
            Future<Boolean> first = executor.submit(attempt);
            Future<Boolean> second = executor.submit(attempt);
            ready.await();
            start.countDown();
            assertEquals(1, (first.get() ? 1 : 0) + (second.get() ? 1 : 0));
        } finally {
            executor.shutdownNow();
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void migrateToVersionFive() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").cleanDisabled(false).load().clean();
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("5").load().migrate();
    }

    private void insert(Connection connection, String seatType, String movieVersion, BigDecimal amount,
            LocalDate validFrom, LocalDate validTo, String status, String origin) throws SQLException {
        String key = movieVersion == null ? null : movieVersion.trim().replaceAll("\\s+", " ").toUpperCase();
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into seat_prices (seat_type, movie_version, movie_version_key, price, valid_from, valid_to, status, origin)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, seatType);
            statement.setString(2, movieVersion == null ? null : movieVersion.trim().replaceAll("\\s+", " "));
            statement.setString(3, key);
            statement.setBigDecimal(4, amount);
            statement.setObject(5, validFrom);
            statement.setObject(6, validTo);
            statement.setString(7, status);
            statement.setString(8, origin);
            statement.executeUpdate();
        }
    }

    private void insertLegacy(Connection connection, String seatType, String movieVersion, BigDecimal amount,
            LocalDate validFrom, LocalDate validTo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into seat_prices (seat_type, movie_version, price, valid_from, valid_to)
                values (?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, seatType);
            statement.setString(2, movieVersion);
            statement.setBigDecimal(3, amount);
            statement.setObject(4, validFrom);
            statement.setObject(5, validTo);
            statement.executeUpdate();
        }
    }
}
