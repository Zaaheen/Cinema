package com.cinema.dto.admin.showtime;

import java.time.OffsetDateTime;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record AdminShowtimePatchRequest(Long roomId, OffsetDateTime startsAt) {
    private static final Set<String> ALLOWED = Set.of("roomId", "startsAt");
    public static AdminShowtimePatchRequest from(JsonNode body) {
        AdminShowtimeCreateRequest.validateObject(body, ALLOWED);
        return new AdminShowtimePatchRequest(AdminShowtimeCreateRequest.requiredPositive(body, "roomId"), AdminShowtimeCreateRequest.requiredOffsetDateTime(body, "startsAt"));
    }
}
