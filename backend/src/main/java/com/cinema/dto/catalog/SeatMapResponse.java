package com.cinema.dto.catalog;

import java.time.Instant;
import java.util.List;

public record SeatMapResponse(
        ShowtimeSeatMapResponse showtime,
        Instant serverTime,
        boolean salesOpen,
        List<SeatMapSeatResponse> seats
) {
    public record ShowtimeSeatMapResponse(
            Long id,
            String movieTitle,
            String roomName,
            Instant startsAt,
            Instant endsAt,
            String status
    ) {}

    public record SeatMapSeatResponse(
            Long id,
            String rowLabel,
            int seatNumber,
            String seatType,
            String availability
    ) {}
}
