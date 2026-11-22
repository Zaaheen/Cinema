package com.cinema.repository;

import com.cinema.entity.BookingSeat;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    boolean existsByShowtimeId(Long showtimeId);

    @Query("""
            select bs from BookingSeat bs
            join fetch bs.booking b
            join fetch b.member m
            join fetch m.user
            where bs.showtime.id = :showtimeId
              and (
                    bs.status = 'CONFIRMED'
                    or (bs.status = 'HELD' and b.status = 'PENDING_PAYMENT' and b.expiresAt > :now)
              )
            """)
    List<BookingSeat> findActiveForSeatMap(
            @Param("showtimeId") Long showtimeId,
            @Param("now") Instant now
    );
}
