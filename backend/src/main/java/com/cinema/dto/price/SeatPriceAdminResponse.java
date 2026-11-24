package com.cinema.dto.price;

import com.cinema.entity.SeatPrice;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record SeatPriceAdminResponse(Long id, String seatType, String movieVersion, BigDecimal amount,
        LocalDate validFrom, LocalDate validTo, String status, String origin, String effectiveState,
        Long rowVersion, Instant createdAt, Instant updatedAt, String createdByUsername,
        String updatedByUsername, List<SeatPriceAuditResponse> auditHistory) {
    public static SeatPriceAdminResponse from(SeatPrice price, String effectiveState,
            List<SeatPriceAuditResponse> auditHistory) {
        return new SeatPriceAdminResponse(price.getId(), price.getSeatType(), price.getMovieVersion(),
                price.getPrice(), price.getValidFrom(), price.getValidTo(), price.getStatus(),
                price.getOrigin(), effectiveState, price.getRowVersion(), price.getCreatedAt(),
                price.getUpdatedAt(), price.getCreatedByUser() == null ? null : price.getCreatedByUser().getUsername(),
                price.getUpdatedByUser() == null ? null : price.getUpdatedByUser().getUsername(), auditHistory);
    }
}
