package com.cinema.repository;

import com.cinema.entity.Showtime;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Showtime s join fetch s.movie join fetch s.room where s.id = :showtimeId")
    java.util.Optional<Showtime> findByIdForUpdate(@Param("showtimeId") Long showtimeId);

    @Query("""
            select s from Showtime s
            join fetch s.movie
            join fetch s.room
            where s.startsAt >= :startOfDay and s.startsAt < :startOfNextDay
            order by s.startsAt asc, s.id asc
            """)
    List<Showtime> findAdminByStartsAtBetween(
            @Param("startOfDay") Instant startOfDay,
            @Param("startOfNextDay") Instant startOfNextDay);

    @Query("select s from Showtime s join fetch s.movie join fetch s.room where s.id = :showtimeId")
    java.util.Optional<Showtime> findAdminById(@Param("showtimeId") Long showtimeId);

    boolean existsByMovieIdAndStatusNot(Long movieId, String status);

    @Query("""
            SELECT s
            FROM Showtime s
            JOIN FETCH s.movie
            JOIN FETCH s.room
            WHERE s.movie.status = 'NOW_SHOWING'
              AND s.room.status = 'ACTIVE'
              AND s.status IN ('SCHEDULED', 'ONGOING')
              AND s.endsAt > :now
            ORDER BY s.startsAt ASC
            """)
    List<Showtime> findBookableFrom(@Param("now") Instant now);

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
