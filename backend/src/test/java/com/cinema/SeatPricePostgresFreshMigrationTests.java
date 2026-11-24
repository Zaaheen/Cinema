package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class SeatPricePostgresFreshMigrationTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_pricing_fresh_test")
            .withUsername("cinema")
            .withPassword("cinema");

    @Test
    void migratesFreshPostgresThroughV9ThenValidatesAndIsIdempotent() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        assertEquals(9, flyway.migrate().migrationsExecuted);
        flyway.validate();
        assertEquals(0, flyway.migrate().migrationsExecuted);

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            assertEquals(Set.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), successfulVersions(connection));
            assertTrue(relationExists(connection, "seat_price_audit_logs"));
            assertTrue(constraintExists(connection, "ex_seat_prices_active_generic_overlap"));
            assertTrue(constraintExists(connection, "ex_seat_prices_active_exact_overlap"));
            assertTrue(constraintExists(connection, "ck_seat_prices_status"));
            assertTrue(constraintExists(connection, "ck_seat_prices_origin"));
        }
    }

    private Set<String> successfulVersions(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select version from flyway_schema_history where success = true and version is not null")) {
            try (ResultSet rows = statement.executeQuery()) {
                java.util.Set<String> versions = new java.util.HashSet<>();
                while (rows.next()) versions.add(rows.getString(1));
                return versions;
            }
        }
    }

    private boolean relationExists(Connection connection, String relation) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement("select to_regclass(?) is not null")) {
            statement.setString(1, relation);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getBoolean(1);
            }
        }
    }

    private boolean constraintExists(Connection connection, String constraint) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "select exists (select 1 from pg_constraint where conname = ?)")) {
            statement.setString(1, constraint);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getBoolean(1);
            }
        }
    }
}
