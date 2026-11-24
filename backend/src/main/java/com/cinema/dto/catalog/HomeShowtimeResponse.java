package com.cinema.dto.catalog;

import com.cinema.entity.Showtime;
import java.time.Instant;

public record HomeShowtimeResponse(Long id, Long movieId, String movieTitle, String moviePosterUrl,
        String movieVersion, String roomName, Instant startsAt, Instant endsAt, String status) {
    public static HomeShowtimeResponse from(Showtime showtime) {
        return new HomeShowtimeResponse(showtime.getId(), showtime.getMovie().getId(),
                showtime.getMovie().getTitle(), showtime.getMovie().getPosterUrl(),
                showtime.getMovie().getVersion(), showtime.getRoom().getName(),
                showtime.getStartsAt(), showtime.getEndsAt(), showtime.getStatus());
    }
}
