package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class ShowtimeOverlapPostgresMigrationTests {
    private static final long READY_TIMEOUT_SECONDS = 15;
    private static final long FUTURE_TIMEOUT_SECONDS = 30;
    private static final long EXECUTOR_TIMEOUT_SECONDS = 15;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_showtime_overlap_test")
            .withUsername("cinema")
            .withPassword("cinema");

    @Test
    void freshDatabaseMigratesThroughV8ValidatesAndIsIdempotent() throws Exception {
        clean();
        Flyway flyway = flyway();
        assertEquals(8, flyway.migrate().migrationsExecuted);
        flyway.validate();
        assertEquals(0, flyway.migrate().migrationsExecuted);

        try (Connection connection = connect()) {
            assertTrue(extensionInstalled(connection, "btree_gist"));
            assertTrue(constraintExists(connection, "ex_showtimes_active_room_occupied"));
            assertTrue(indexExists(connection, "ix_showtimes_room"));
            assertTrue(constraintExists(connection, "ex_seat_prices_active_generic_overlap"));
            assertTrue(constraintExists(connection, "ex_seat_prices_active_exact_overlap"));
            assertEquals(6, count(connection, "select count(*) from genres"));
            assertEquals(8, successfulMigrationCount(connection));
        }
    }

    @Test
    void upgradesV1ThroughV7ToV8WithoutChangingExistingShowtimeOrBookingData() throws Exception {
        migrateTo("7");
        try (Connection connection = connect()) {
            long movie = insertMovie(connection, "Upgrade movie");
            long room = insertRoom(connection, "Upgrade room");
            long showtime = insertShowtime(connection, movie, room, "2027-03-01T03:00:00Z", "2027-03-01T05:00:00Z", "SCHEDULED");
            long bookingSeat = insertBookingTicketHistory(connection, showtime, room);

            migrateLatest();

            assertEquals("SCHEDULED", showtimeStatus(connection, showtime));
            assertEquals(1, count(connection, "select count(*) from bookings where showtime_id = " + showtime));
            assertEquals(1, count(connection, "select count(*) from booking_seats where id = " + bookingSeat));
            assertEquals(1, count(connection, "select count(*) from tickets where booking_seat_id = " + bookingSeat));
            assertTrue(constraintExists(connection, "ex_showtimes_active_room_occupied"));
        }
    }

    @Test
    void rejectsSameRoomOverlapAndFourteenMinuteTurnaroundButAllowsExactlyFifteenMinutes() throws Exception {
        migrateLatestFromClean();
        try (Connection connection = connect()) {
            long movie = insertMovie(connection, "Overlap movie");
            long room = insertRoom(connection, "Overlap room");
            insertShowtime(connection, movie, room, "2027-03-02T03:00:00Z", "2027-03-02T05:00:00Z", "SCHEDULED");

            SQLException overlap = assertThrows(SQLException.class, () -> insertShowtime(connection, movie, room,
                    "2027-03-02T04:30:00Z", "2027-03-02T06:00:00Z", "SCHEDULED"));
            assertEquals("23P01", sqlState(overlap));
            SQLException shortTurnaround = assertThrows(SQLException.class, () -> insertShowtime(connection, movie, room,
                    "2027-03-02T05:14:00Z", "2027-03-02T06:00:00Z", "SCHEDULED"));
            assertEquals("23P01", sqlState(shortTurnaround));

            insertShowtime(connection, movie, room, "2027-03-02T05:15:00Z", "2027-03-02T06:00:00Z", "SCHEDULED");
        }
    }

    @Test
    void allowsDifferentRoomsAndHistoricalStatuses() throws Exception {
        migrateLatestFromClean();
        try (Connection connection = connect()) {
            long movie = insertMovie(connection, "Status movie");
            long roomOne = insertRoom(connection, "Status room one");
            long roomTwo = insertRoom(connection, "Status room two");
            insertShowtime(connection, movie, roomOne, "2027-03-03T03:00:00Z", "2027-03-03T05:00:00Z", "SCHEDULED");
            insertShowtime(connection, movie, roomTwo, "2027-03-03T03:00:00Z", "2027-03-03T05:00:00Z", "SCHEDULED");
            insertShowtime(connection, movie, roomOne, "2027-03-03T03:30:00Z", "2027-03-03T04:30:00Z", "CANCELLED");
            insertShowtime(connection, movie, roomOne, "2027-03-03T04:00:00Z", "2027-03-03T05:00:00Z", "FINISHED");
        }
    }

    @Test
    void rejectsRoomChangeAndStatusTransitionThatWouldCreateAnActiveConflict() throws Exception {
        migrateLatestFromClean();
        try (Connection connection = connect()) {
            long movie = insertMovie(connection, "Transition movie");
            long roomOne = insertRoom(connection, "Transition room one");
            long roomTwo = insertRoom(connection, "Transition room two");
            insertShowtime(connection, movie, roomOne, "2027-03-04T03:00:00Z", "2027-03-04T05:00:00Z", "SCHEDULED");
            long otherRoom = insertShowtime(connection, movie, roomTwo, "2027-03-04T03:30:00Z", "2027-03-04T04:30:00Z", "SCHEDULED");
            long cancelled = insertShowtime(connection, movie, roomOne, "2027-03-04T03:30:00Z", "2027-03-04T04:30:00Z", "CANCELLED");

            SQLException roomConflict = assertThrows(SQLException.class,
                    () -> updateRoom(connection, otherRoom, roomOne));
            assertEquals("23P01", sqlState(roomConflict));
            SQLException statusConflict = assertThrows(SQLException.class,
                    () -> updateStatus(connection, cancelled, "ONGOING"));
            assertEquals("23P01", sqlState(statusConflict));
        }
    }

    @Test
    void utcNormalizedConstraintIsIndependentOfSessionTimezoneAndMidnightBoundaries() throws Exception {
        for (String sessionTimezone : List.of("UTC", "Asia/Ho_Chi_Minh", "America/New_York")) {
            assertTurnaroundBehavior(sessionTimezone, "2027-04-01T23:00:00Z", "2027-04-02T01:00:00Z");
        }
        assertTurnaroundBehavior("Asia/Ho_Chi_Minh", "2027-04-01T23:30:00+07:00", "2027-04-02T01:30:00+07:00");
    }

    @Test
    void concurrentConflictingInsertsAndUpdatesAllowAtMostOneCommit() throws Exception {
        migrateLatestFromClean();
        try (Connection connection = connect()) {
            long movie = insertMovie(connection, "Concurrent movie");
            long room = insertRoom(connection, "Concurrent room");
            assertExactlyOneCommitAndOneExclusionConflict(concurrentConflictingInserts(movie, room,
                    "2027-03-05T03:00:00Z", "2027-03-05T05:00:00Z"));

            long firstRoom = insertRoom(connection, "Concurrent update room one");
            long secondRoom = insertRoom(connection, "Concurrent update room two");
            long targetRoom = insertRoom(connection, "Concurrent update target room");
            long first = insertShowtime(connection, movie, firstRoom, "2027-03-06T03:00:00Z", "2027-03-06T04:00:00Z", "SCHEDULED");
            long second = insertShowtime(connection, movie, secondRoom, "2027-03-06T03:00:00Z", "2027-03-06T04:00:00Z", "SCHEDULED");
            assertExactlyOneCommitAndOneExclusionConflict(
                    concurrentConflictingRoomUpdates(first, second, targetRoom));
        }
    }

    @Test
    void refusesV8WhenExistingActiveRowsConflictAndLeavesRowsUnchanged() throws Exception {
        migrateTo("7");
        try (Connection connection = connect()) {
            long movie = insertMovie(connection, "Failure movie");
            long room = insertRoom(connection, "Failure room");
            long first = insertShowtime(connection, movie, room, "2027-03-07T03:00:00Z", "2027-03-07T05:00:00Z", "SCHEDULED");
            long second = insertShowtime(connection, movie, room, "2027-03-07T04:00:00Z", "2027-03-07T06:00:00Z", "ONGOING");

            assertThrows(FlywayException.class, this::migrateLatest);
            assertEquals("SCHEDULED", showtimeStatus(connection, first));
            assertEquals("ONGOING", showtimeStatus(connection, second));
            assertFalse(constraintExists(connection, "ex_showtimes_active_room_occupied"));
        }
    }

    private void migrateLatestFromClean() {
        clean();
        migrateLatest();
    }

    private void clean() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").cleanDisabled(false).load().clean();
    }

    private void migrateTo(String version) {
        clean();
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target(version).load().migrate();
    }

    private void migrateLatest() { flyway().migrate(); }

    private Flyway flyway() {
        return Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void assertTurnaroundBehavior(String sessionTimezone, String start, String end) throws Exception {
        migrateLatestFromClean();
        Instant expectedStart = parseInstant(start);
        Instant expectedEnd = parseInstant(end);
        try (Connection connection = connect()) {
            setSessionTimezone(connection, sessionTimezone);
            long movie = insertMovie(connection, "Timezone " + sessionTimezone);
            long room = insertRoom(connection, "Timezone room " + sessionTimezone);
            insertShowtime(connection, movie, room, start, end, "SCHEDULED");

            SQLException shortTurnaround = assertThrows(SQLException.class, () -> insertShowtime(connection, movie, room,
                    expectedEnd.plusSeconds(14 * 60).toString(), expectedEnd.plusSeconds(74 * 60).toString(), "SCHEDULED"));
            assertEquals("23P01", sqlState(shortTurnaround));
            insertShowtime(connection, movie, room, expectedEnd.plusSeconds(15 * 60).toString(),
                    expectedEnd.plusSeconds(75 * 60).toString(), "SCHEDULED");
            assertEquals(expectedStart, showtimeInstant(connection, "starts_at", movie, room));
            assertEquals(expectedEnd, showtimeInstant(connection, "ends_at", movie, room));
        }
    }

    private void setSessionTimezone(Connection connection, String timezone) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select set_config('TimeZone', ?, false)")) {
            statement.setString(1, timezone);
            statement.execute();
        }
    }

    private long insertMovie(Connection connection, String title) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into movies (title, duration_minutes, version, status)
                values (?, 120, '2D', 'NOW_SHOWING') returning id
                """)) {
            statement.setString(1, title);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    private long insertRoom(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("insert into rooms (name) values (?) returning id")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    private long insertShowtime(Connection connection, long movieId, long roomId, String start, String end, String status)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into showtimes (movie_id, room_id, starts_at, ends_at, status)
                values (?, ?, ?, ?, ?) returning id
                """)) {
            statement.setLong(1, movieId);
            statement.setLong(2, roomId);
            statement.setTimestamp(3, Timestamp.from(parseInstant(start)));
            statement.setTimestamp(4, Timestamp.from(parseInstant(end)));
            statement.setString(5, status);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getLong(1); }
        }
    }

    private long insertBookingTicketHistory(Connection connection, long showtimeId, long roomId) throws SQLException {
        long role;
        try (PreparedStatement statement = connection.prepareStatement(
                "select id from roles where code = 'MEMBER'")) {
            try (ResultSet rows = statement.executeQuery()) { rows.next(); role = rows.getLong(1); }
        }
        long user;
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into users (username, password_hash, full_name, role_id)
                values ('v8-history-user', 'test', 'V8 history', ?) returning id
                """)) {
            statement.setLong(1, role);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); user = rows.getLong(1); }
        }
        long member;
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into members (user_id, member_code) values (?, 'V8HISTORY') returning id
                """)) {
            statement.setLong(1, user);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); member = rows.getLong(1); }
        }
        long booking;
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into bookings (booking_code, member_id, showtime_id, status, subtotal, total_amount)
                values ('V8-HISTORY', ?, ?, 'PAID', 100000, 100000) returning id
                """)) {
            statement.setLong(1, member); statement.setLong(2, showtimeId);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); booking = rows.getLong(1); }
        }
        long seat;
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into seats (room_id, row_label, seat_number) values (?, 'A', 1) returning id
                """)) {
            statement.setLong(1, roomId);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); seat = rows.getLong(1); }
        }
        long bookingSeat;
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into booking_seats (booking_id, showtime_id, seat_id, unit_price, status)
                values (?, ?, ?, 100000, 'CONFIRMED') returning id
                """)) {
            statement.setLong(1, booking); statement.setLong(2, showtimeId); statement.setLong(3, seat);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); bookingSeat = rows.getLong(1); }
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into tickets (booking_seat_id, ticket_code) values (?, 'V8-HISTORY-TICKET')
                """)) { statement.setLong(1, bookingSeat); statement.executeUpdate(); }
        return bookingSeat;
    }

    private void updateRoom(Connection connection, long showtimeId, long roomId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("update showtimes set room_id = ? where id = ?")) {
            statement.setLong(1, roomId); statement.setLong(2, showtimeId); statement.executeUpdate();
        }
    }

    private void updateStatus(Connection connection, long showtimeId, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("update showtimes set status = ? where id = ?")) {
            statement.setString(1, status); statement.setLong(2, showtimeId); statement.executeUpdate();
        }
    }

    private List<AttemptResult> concurrentConflictingInserts(long movieId, long roomId, String start, String end)
            throws Exception {
        CountDownLatch firstInserted = new CountDownLatch(1);
        CountDownLatch secondIssued = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<AttemptResult> first = null;
        Future<AttemptResult> second = null;
        try {
            first = executor.submit(() -> insertThenCommitAfterSecondIsIssued(
                    movieId, roomId, start, end, firstInserted, secondIssued));
            second = executor.submit(() -> insertAfterFirstInsert(
                    movieId, roomId, start, end, firstInserted, secondIssued));
            return List.of(first.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    second.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } finally {
            firstInserted.countDown();
            secondIssued.countDown();
            if (first != null && !first.isDone()) first.cancel(true);
            if (second != null && !second.isDone()) second.cancel(true);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(EXECUTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "concurrent insert executor did not terminate");
        }
    }

    private AttemptResult insertThenCommitAfterSecondIsIssued(long movieId, long roomId, String start, String end,
            CountDownLatch firstInserted, CountDownLatch secondIssued) throws Exception {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            configureTransactionTimeouts(connection);
            try {
                insertShowtime(connection, movieId, roomId, start, end, "SCHEDULED");
                firstInserted.countDown();
                assertTrue(secondIssued.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "second insert was not issued");
                connection.commit();
                return AttemptResult.success();
            } catch (SQLException exception) {
                rollback(connection);
                return AttemptResult.failure(sqlState(exception));
            }
        }
    }

    private AttemptResult insertAfterFirstInsert(long movieId, long roomId, String start, String end,
            CountDownLatch firstInserted, CountDownLatch secondIssued) throws Exception {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            configureTransactionTimeouts(connection);
            try {
                assertTrue(firstInserted.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "first insert was not prepared");
                secondIssued.countDown();
                insertShowtime(connection, movieId, roomId, start, end, "SCHEDULED");
                connection.commit();
                return AttemptResult.success();
            } catch (SQLException exception) {
                rollback(connection);
                return AttemptResult.failure(sqlState(exception));
            }
        }
    }

    private List<AttemptResult> concurrentConflictingRoomUpdates(long firstShowtimeId, long secondShowtimeId, long targetRoomId)
            throws Exception {
        CountDownLatch firstUpdated = new CountDownLatch(1);
        CountDownLatch secondIssued = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<AttemptResult> first = null;
        Future<AttemptResult> second = null;
        try {
            first = executor.submit(() -> updateRoomThenCommitAfterSecondIsIssued(
                    firstShowtimeId, targetRoomId, firstUpdated, secondIssued));
            second = executor.submit(() -> updateRoomAfterFirstUpdate(
                    secondShowtimeId, targetRoomId, firstUpdated, secondIssued));
            return List.of(first.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    second.get(FUTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } finally {
            firstUpdated.countDown();
            secondIssued.countDown();
            if (first != null && !first.isDone()) first.cancel(true);
            if (second != null && !second.isDone()) second.cancel(true);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(EXECUTOR_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "concurrent update executor did not terminate");
        }
    }

    private AttemptResult updateRoomThenCommitAfterSecondIsIssued(long showtimeId, long roomId,
            CountDownLatch firstUpdated, CountDownLatch secondIssued) throws Exception {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            configureTransactionTimeouts(connection);
            try {
                updateRoomInTransaction(connection, showtimeId, roomId);
                firstUpdated.countDown();
                assertTrue(secondIssued.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "second update was not issued");
                connection.commit();
                return AttemptResult.success();
            } catch (SQLException exception) {
                rollback(connection);
                return AttemptResult.failure(sqlState(exception));
            }
        }
    }

    private AttemptResult updateRoomAfterFirstUpdate(long showtimeId, long roomId,
            CountDownLatch firstUpdated, CountDownLatch secondIssued) throws Exception {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            configureTransactionTimeouts(connection);
            try {
                assertTrue(firstUpdated.await(READY_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                        "first update was not prepared");
                secondIssued.countDown();
                updateRoomInTransaction(connection, showtimeId, roomId);
                connection.commit();
                return AttemptResult.success();
            } catch (SQLException exception) {
                rollback(connection);
                return AttemptResult.failure(sqlState(exception));
            }
        }
    }

    private void updateRoomInTransaction(Connection connection, long showtimeId, long roomId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("update showtimes set room_id = ? where id = ?")) {
            statement.setLong(1, roomId);
            statement.setLong(2, showtimeId);
            statement.executeUpdate();
        }
    }

    private void assertExactlyOneCommitAndOneExclusionConflict(List<AttemptResult> results) {
        assertEquals(1, results.stream().filter(AttemptResult::committed).count());
        AttemptResult conflict = results.stream().filter(result -> !result.committed()).findFirst().orElseThrow();
        assertEquals("23P01", conflict.sqlState());
    }

    private void configureTransactionTimeouts(Connection connection) throws SQLException {
        try (PreparedStatement lockTimeout = connection.prepareStatement("set local lock_timeout = '10s'")) {
            lockTimeout.execute();
        }
        try (PreparedStatement statementTimeout = connection.prepareStatement("set local statement_timeout = '20s'")) {
            statementTimeout.execute();
        }
    }

    private void rollback(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) connection.rollback();
    }

    private String showtimeStatus(Connection connection, long showtimeId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select status from showtimes where id = ?")) {
            statement.setLong(1, showtimeId);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getString(1); }
        }
    }

    private Instant showtimeInstant(Connection connection, String column, long movieId, long roomId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select " + column + " from showtimes where movie_id = ? and room_id = ? order by id limit 1")) {
            statement.setLong(1, movieId);
            statement.setLong(2, roomId);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getTimestamp(1).toInstant(); }
        }
    }

    private Instant parseInstant(String value) { return OffsetDateTime.parse(value).toInstant(); }

    private long count(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet rows = statement.executeQuery()) {
            rows.next(); return rows.getLong(1);
        }
    }

    private boolean extensionInstalled(Connection connection, String extension) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select exists (select 1 from pg_extension where extname = ?)")) {
            statement.setString(1, extension);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getBoolean(1); }
        }
    }

    private boolean constraintExists(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select exists (select 1 from pg_constraint where conname = ?)")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getBoolean(1); }
        }
    }

    private boolean indexExists(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select to_regclass(?) is not null")) {
            statement.setString(1, name);
            try (ResultSet rows = statement.executeQuery()) { rows.next(); return rows.getBoolean(1); }
        }
    }

    private long successfulMigrationCount(Connection connection) throws SQLException {
        return count(connection, "select count(*) from flyway_schema_history where success and version is not null");
    }

    private String sqlState(SQLException exception) {
        SQLException current = exception;
        while (current != null) {
            if (current.getSQLState() != null) return current.getSQLState();
            current = current.getNextException();
        }
        return null;
    }

    private record AttemptResult(boolean committed, String sqlState) {
        static AttemptResult success() { return new AttemptResult(true, null); }
        static AttemptResult failure(String sqlState) { return new AttemptResult(false, sqlState); }
    }
}
