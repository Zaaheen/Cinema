package com.cinema.repository;

import com.cinema.entity.Showtime;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    @Query("""
            SELECT DISTINCT s
            FROM Showtime s
            JOIN FETCH s.room
            JOIN FETCH s.movie
            WHERE s.startsAt < :endOfDay
              AND s.endsAt > :startOfDay
            ORDER BY s.startsAt ASC
            """)
    List<Showtime> findAllOverlappingDay(
            @Param("startOfDay") Instant startOfDay,
            @Param("endOfDay") Instant endOfDay);

    @Query("""
            SELECT s
            FROM Showtime s
            JOIN FETCH s.room
            WHERE s.movie.id = :movieId
              AND s.status = 'SCHEDULED'
              AND s.startsAt > :now
            ORDER BY s.room.id, s.startsAt
            """)
    List<Showtime> findUpcomingScheduledByMovie(
            @Param("movieId") Long movieId,
            @Param("now") Instant now);

    @Query("""
            SELECT s
            FROM Showtime s
            WHERE s.room.id = :roomId
              AND s.status IN ('SCHEDULED', 'ONGOING')
              AND s.startsAt < :candidateEndWithBuffer
              AND s.endsAt > :candidateStartWithBuffer
            ORDER BY s.startsAt ASC
            """)
    List<Showtime> findRoomConflicts(
            @Param("roomId") Long roomId,
            @Param("candidateStartWithBuffer") Instant candidateStartWithBuffer,
            @Param("candidateEndWithBuffer") Instant candidateEndWithBuffer);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Showtime s where s.id = :showtimeId")
    Optional<Showtime> findByIdForUpdate(@Param("showtimeId") Long showtimeId);

    @Query("""
            SELECT s
            FROM Showtime s
            JOIN FETCH s.room
            WHERE s.movie.id = :movieId
              AND s.status = 'SCHEDULED'
              AND s.startsAt >= :startOfDay
              AND s.startsAt < :startOfNextDay
              AND s.startsAt > :salesCloseThreshold
            ORDER BY s.startsAt ASC
            """)
    List<Showtime> findScheduledShowtimesByMovieAndDate(
            @Param("movieId") Long movieId,
            @Param("startOfDay") Instant startOfDay,
            @Param("startOfNextDay") Instant startOfNextDay,
            @Param("salesCloseThreshold") Instant salesCloseThreshold
    );
}
