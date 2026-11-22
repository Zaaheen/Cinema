package com.cinema.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cinema.dto.admin.showtime.AdminShowtimePatchRequest;
import com.cinema.dto.admin.showtime.AdminShowtimeResponse;
import com.cinema.entity.Movie;
import com.cinema.entity.Room;
import com.cinema.entity.Showtime;
import com.cinema.exception.BusinessException;
import com.cinema.repository.BookingRepository;
import com.cinema.repository.BookingSeatRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.RoomRepository;
import com.cinema.repository.ShowtimeRepository;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class AdminShowtimeDeadlockRetryTests {
    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Test
    void retriesFirstPostgresDeadlockInFreshTransactionAndRevalidatesHistory() {
        Harness harness = new Harness();
        Showtime first = harness.showtime();
        Showtime second = harness.showtime();
        when(harness.showtimes.findByIdForUpdate(1L)).thenReturn(Optional.of(first), Optional.of(second));
        when(harness.rooms.findById(2L)).thenReturn(Optional.of(harness.room));
        when(harness.bookings.existsByShowtimeId(1L)).thenReturn(false);
        when(harness.bookingSeats.existsByShowtimeId(1L)).thenReturn(false);
        when(harness.showtimes.saveAndFlush(first)).thenThrow(deadlock());
        when(harness.showtimes.saveAndFlush(second)).thenReturn(second);

        AdminShowtimeResponse response = harness.service.update(1L, harness.request());

        assertEquals(second.getId(), response.id());
        assertEquals(2, harness.transactions.begins);
        assertEquals(1, harness.transactions.rollbacks);
        assertEquals(1, harness.transactions.commits);
        verify(harness.showtimes, times(2)).findByIdForUpdate(1L);
        verify(harness.bookings, times(2)).existsByShowtimeId(1L);
        verify(harness.bookingSeats, times(2)).existsByShowtimeId(1L);
        assertSame(harness.room, second.getRoom());
    }

    @Test
    void stopsAfterThreeDeadlocksWithoutCommittingOrMappingToRoomConflict() {
        Harness harness = new Harness();
        when(harness.showtimes.findByIdForUpdate(1L)).thenReturn(Optional.of(harness.showtime()));
        when(harness.rooms.findById(2L)).thenReturn(Optional.of(harness.room));
        when(harness.bookings.existsByShowtimeId(1L)).thenReturn(false);
        when(harness.bookingSeats.existsByShowtimeId(1L)).thenReturn(false);
        when(harness.showtimes.saveAndFlush(org.mockito.ArgumentMatchers.any(Showtime.class))).thenThrow(deadlock());

        DataIntegrityViolationException failure = assertThrows(DataIntegrityViolationException.class,
                () -> harness.service.update(1L, harness.request()));

        assertEquals("40P01", ((SQLException) failure.getCause()).getSQLState());
        assertEquals(3, harness.transactions.begins);
        assertEquals(3, harness.transactions.rollbacks);
        assertEquals(0, harness.transactions.commits);
        verify(harness.showtimes, times(3)).findByIdForUpdate(1L);
        verify(harness.showtimes, times(3)).saveAndFlush(org.mockito.ArgumentMatchers.any(Showtime.class));
    }

    @Test
    void doesNotRetryOtherDatabaseErrorsAndRerunHistoryCanBlockRetry() {
        Harness databaseError = new Harness();
        when(databaseError.showtimes.findByIdForUpdate(1L)).thenReturn(Optional.of(databaseError.showtime()));
        when(databaseError.rooms.findById(2L)).thenReturn(Optional.of(databaseError.room));
        when(databaseError.bookings.existsByShowtimeId(1L)).thenReturn(false);
        when(databaseError.bookingSeats.existsByShowtimeId(1L)).thenReturn(false);
        DataIntegrityViolationException other = new DataIntegrityViolationException("other", new SQLException("other", "23505"));
        when(databaseError.showtimes.saveAndFlush(org.mockito.ArgumentMatchers.any(Showtime.class))).thenThrow(other);

        assertSame(other, assertThrows(DataIntegrityViolationException.class,
                () -> databaseError.service.update(1L, databaseError.request())));
        assertEquals(1, databaseError.transactions.begins);

        Harness history = new Harness();
        when(history.showtimes.findByIdForUpdate(1L)).thenReturn(Optional.of(history.showtime()), Optional.of(history.showtime()));
        when(history.rooms.findById(2L)).thenReturn(Optional.of(history.room));
        when(history.bookings.existsByShowtimeId(1L)).thenReturn(false, true);
        when(history.bookingSeats.existsByShowtimeId(1L)).thenReturn(false);
        when(history.showtimes.saveAndFlush(org.mockito.ArgumentMatchers.any(Showtime.class))).thenThrow(deadlock());

        BusinessException blocked = assertThrows(BusinessException.class,
                () -> history.service.update(1L, history.request()));
        assertEquals("SHOWTIME_HAS_BOOKING_HISTORY", blocked.getMessage());
        assertEquals(2, history.transactions.begins);
        assertEquals(2, history.transactions.rollbacks);
        verify(history.bookings, times(2)).existsByShowtimeId(1L);
        verify(history.showtimes, times(1)).saveAndFlush(org.mockito.ArgumentMatchers.any(Showtime.class));
    }

    private static DataIntegrityViolationException deadlock() {
        return new DataIntegrityViolationException("deadlock", new SQLException("deadlock", "40P01"));
    }

    private static final class Harness {
        final ShowtimeRepository showtimes = mock(ShowtimeRepository.class);
        final MovieRepository movies = mock(MovieRepository.class);
        final RoomRepository rooms = mock(RoomRepository.class);
        final BookingRepository bookings = mock(BookingRepository.class);
        final BookingSeatRepository bookingSeats = mock(BookingSeatRepository.class);
        final CountingTransactionManager transactions = new CountingTransactionManager();
        final Room room = room();
        final AdminShowtimeServiceImpl service = new AdminShowtimeServiceImpl(showtimes, movies, rooms, bookings,
                bookingSeats, new TransactionTemplate(transactions));

        AdminShowtimePatchRequest request() {
            return new AdminShowtimePatchRequest(2L, OffsetDateTime.ofInstant(nextStart(), CINEMA_ZONE));
        }

        Showtime showtime() {
            Movie movie = new Movie();
            movie.setDurationMinutes(90);
            Showtime showtime = new Showtime();
            showtime.setId(1L);
            showtime.setMovie(movie);
            showtime.setRoom(room);
            showtime.setStatus("SCHEDULED");
            showtime.setStartsAt(nextStart().minusSeconds(3600));
            showtime.setEndsAt(nextStart());
            return showtime;
        }

        private static Room room() {
            Room value = new Room();
            value.setId(2L);
            value.setStatus("ACTIVE");
            return value;
        }
    }

    private static Instant nextStart() {
        return LocalDate.now(CINEMA_ZONE).plusDays(2).atTime(10, 0).atZone(CINEMA_ZONE).toInstant();
    }

    private static final class CountingTransactionManager extends AbstractPlatformTransactionManager {
        int begins;
        int commits;
        int rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            begins++;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}
