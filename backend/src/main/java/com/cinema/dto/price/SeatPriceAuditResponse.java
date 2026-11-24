package com.cinema.dto.price;

import com.cinema.entity.SeatPriceAuditLog;
import java.time.Instant;

public record SeatPriceAuditResponse(Long id, long revision, String action, String actorUsername,
        String beforeState, String afterState, Instant createdAt) {
    public static SeatPriceAuditResponse from(SeatPriceAuditLog item) {
        return new SeatPriceAuditResponse(item.getId(), item.getRevision(), item.getAction(),
                item.getActorUser() == null ? null : item.getActorUser().getUsername(),
                item.getBeforeState(), item.getAfterState(), item.getCreatedAt());
    }
}
