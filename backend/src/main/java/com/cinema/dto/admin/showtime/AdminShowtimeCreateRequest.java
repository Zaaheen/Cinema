package com.cinema.dto.admin.showtime;

import com.cinema.exception.BusinessException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public record AdminShowtimeCreateRequest(Long movieId, Long roomId, OffsetDateTime startsAt) {
    private static final Set<String> ALLOWED = Set.of("movieId", "roomId", "startsAt");
    public static AdminShowtimeCreateRequest from(JsonNode body) {
        validateObject(body, ALLOWED);
        return new AdminShowtimeCreateRequest(requiredPositive(body, "movieId"), requiredPositive(body, "roomId"), requiredOffsetDateTime(body, "startsAt"));
    }
    static void validateObject(JsonNode body, Set<String> allowed) {
        if (body == null || !body.isObject()) throw BusinessException.badRequest("Nội dung yêu cầu không hợp lệ");
        for (String field : body.propertyNames()) {
            if (!allowed.contains(field)) throw BusinessException.badRequest("Trường không hỗ trợ: " + field);
            if (body.get(field).isNull()) throw BusinessException.badRequest(field + " không được null");
        }
    }
    static Long requiredPositive(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || !value.canConvertToLong() || value.longValue() <= 0) throw BusinessException.badRequest(field + " phải là số nguyên dương");
        return value.longValue();
    }
    static OffsetDateTime requiredOffsetDateTime(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || !value.isTextual()) throw BusinessException.badRequest(field + " phải là thời điểm có offset");
        try { return OffsetDateTime.parse(value.textValue()); }
        catch (DateTimeParseException exception) { throw BusinessException.badRequest(field + " phải là thời điểm ISO-8601 có offset"); }
    }
}
