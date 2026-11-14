
package com.cinema.dto.catalog;

import com.cinema.entity.Showtime;
import java.time.Instant;

public record ShowtimeResponse(
        Long id,
        Instant startsAt,
        Instant endsAt,
        String roomName,
        String status
) {

    public static ShowtimeResponse from(Showtime showtime) {
        return new ShowtimeResponse(
                showtime.getId(),
                showtime.getStartsAt(),
                showtime.getEndsAt(),
                showtime.getRoom().getName(),
                showtime.getStatus()
        );
    }
}
