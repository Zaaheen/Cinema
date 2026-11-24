package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class MainSchemaUpgradePostgresTests {
    @Test
    void upgradesMainV1ThroughV3WithExistingDataToV9() throws Exception {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("cinema_main_upgrade_test")) {
            postgres.start();
            Flyway first = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").target("3").load();
            assertEquals(3, first.migrate().migrationsExecuted);
            first.validate();

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement sql = connection.createStatement()) {
                sql.executeUpdate("insert into movies (title, genre, duration_minutes, version, status) "
                        + "values ('Main upgrade movie', 'Hành động', 120, '2D', 'NOW_SHOWING')");
                sql.executeUpdate("insert into rooms (name) values ('Main upgrade room')");
                sql.executeUpdate("insert into showtimes (movie_id, room_id, starts_at, ends_at) "
                        + "values ((select id from movies where title = 'Main upgrade movie'), "
                        + "(select id from rooms where name = 'Main upgrade room'), "
                        + "'2027-05-01T10:00:00Z', '2027-05-01T12:00:00Z')");
                sql.executeUpdate("insert into seat_prices (seat_type, price, valid_from) "
                        + "values ('STANDARD', 70000.00, '2027-01-01')");
            }

            Flyway latest = Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration").load();
            assertEquals(6, latest.migrate().migrationsExecuted);
            latest.validate();
            assertEquals(0, latest.migrate().migrationsExecuted);

            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement sql = connection.createStatement()) {
                assertEquals(9, count(sql, "select count(*) from flyway_schema_history where success and version is not null"));
                assertEquals(5, count(sql, "select count(*) from rooms where name like 'Phòng 0%' and status = 'ACTIVE'"));
                assertEquals(1, count(sql, "select count(*) from rooms where name = 'Main upgrade room'"));
                assertEquals(1, count(sql, "select count(*) from movies where title = 'Main upgrade movie' and genre = 'Hành động'"));
                assertEquals(1, count(sql, "select count(*) from movie_genres mg join movies m on m.id = mg.movie_id "
                        + "join genres g on g.id = mg.genre_id where m.title = 'Main upgrade movie' and g.name = 'Hành động'"));
                assertEquals(1, count(sql, "select count(*) from showtimes where status = 'SCHEDULED'"));
                assertEquals(1, count(sql, "select count(*) from seat_prices where price = 70000.00 "
                        + "and status = 'INACTIVE' and origin = 'LEGACY_UNREVIEWED'"));
                assertEquals(1, count(sql, "select count(*) from pg_constraint where conname = 'ex_showtimes_active_room_occupied'"));
                assertEquals(1, count(sql, "select count(*) from pg_constraint where conname = 'ex_seat_prices_active_generic_overlap'"));
                assertTrue(count(sql, "select count(*) from pg_extension where extname = 'btree_gist'") > 0);
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
