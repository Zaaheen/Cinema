package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class RoomSeedPostgresMigrationTests {
    @Test
    void v9AddsOnlyMissingRoomsAndPreservesOperatorRoomStatus() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").target("8").load().migrate();

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement sql = connection.createStatement()) {
                sql.executeUpdate("insert into rooms (name, status) values ('Phòng 01 - IMAX Laser 3D', 'INACTIVE')");
                sql.executeUpdate("insert into rooms (name, status) values ('Operator room', 'ACTIVE')");
            }

            Flyway latest = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").load();
            assertEquals(1, latest.migrate().migrationsExecuted);
            latest.validate();
            assertEquals(0, latest.migrate().migrationsExecuted);

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement sql = connection.createStatement()) {
                assertEquals(6, count(sql, "select count(*) from rooms"));
                assertEquals(1, count(sql, "select count(*) from rooms where name = 'Phòng 01 - IMAX Laser 3D' and status = 'INACTIVE'"));
                assertEquals(1, count(sql, "select count(*) from rooms where name = 'Operator room' and status = 'ACTIVE'"));
                assertEquals(0, count(sql, "select count(*) from seats"));
                assertEquals(0, count(sql, "select count(*) from showtimes"));
            }
        }
    }

    private long count(Statement sql, String query) throws Exception {
        try (ResultSet rows = sql.executeQuery(query)) {
            rows.next();
            return rows.getLong(1);
        }
    }
}
