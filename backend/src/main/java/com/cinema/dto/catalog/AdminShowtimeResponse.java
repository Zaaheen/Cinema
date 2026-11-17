package com.cinema.dto.catalog;

import com.cinema.entity.Showtime;
import java.time.Instant;

public record AdminShowtimeResponse(
        Long id,
        Long movieId,
        String movieTitle,
        Long roomId,
        String roomName,
        Instant startsAt,
        Instant endsAt,
        int durationMinutes,
        String status,
        boolean hasBookings
) {
    public static AdminShowtimeResponse from(Showtime showtime, boolean hasBookings) {
        return new AdminShowtimeResponse(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                showtime.getRoom().getId(),
                showtime.getRoom().getName(),
                showtime.getStartsAt(),
                showtime.getEndsAt(),
                showtime.getMovie().getDurationMinutes(),
                showtime.getStatus(),
                hasBookings);
    }
}
