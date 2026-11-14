package com.cinema.repository;

import com.cinema.entity.Showtime;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

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