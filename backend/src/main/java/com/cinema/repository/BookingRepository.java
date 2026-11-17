package com.cinema.repository;

import com.cinema.entity.Booking;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @Query("select case when count(booking) > 0 then true else false end from Booking booking where booking.showtime.id = :showtimeId")
    boolean existsForShowtime(@Param("showtimeId") Long showtimeId);

    @Query("select distinct booking.showtime.id from Booking booking where booking.showtime.id in :showtimeIds")
    List<Long> findShowtimeIdsWithBookings(@Param("showtimeIds") Collection<Long> showtimeIds);
}
