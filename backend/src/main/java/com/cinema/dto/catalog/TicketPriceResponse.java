package com.cinema.dto.catalog;

import com.cinema.entity.SeatPrice;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TicketPriceResponse(Long id, String seatType, String movieVersion, BigDecimal price,
        LocalDate validFrom, LocalDate validTo) {
    public static TicketPriceResponse from(SeatPrice seatPrice) {
        return new TicketPriceResponse(seatPrice.getId(), seatPrice.getSeatType(), seatPrice.getMovieVersion(),
                seatPrice.getPrice(), seatPrice.getValidFrom(), seatPrice.getValidTo());
    }
}
