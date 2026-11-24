package com.cinema.dto.admin.showtime;

import com.cinema.entity.Showtime;
import java.time.Instant;

public record AdminShowtimeResponse(Long id, Long movieId, String movieTitle, int durationMinutes, Long roomId,
        String roomName, Instant startsAt, Instant endsAt, String status, boolean hasBookingHistory) {
    public static AdminShowtimeResponse from(Showtime showtime, boolean hasBookingHistory) {
        return new AdminShowtimeResponse(showtime.getId(), showtime.getMovie().getId(), showtime.getMovie().getTitle(),
                showtime.getMovie().getDurationMinutes(), showtime.getRoom().getId(), showtime.getRoom().getName(),
                showtime.getStartsAt(), showtime.getEndsAt(), showtime.getStatus(), hasBookingHistory);
    }
}
