package com.cinema.dto.admin.movie;

import com.cinema.exception.BusinessException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Missing fields are null; this parser rejects explicit JSON null without changing global Jackson behavior. */
public record AdminMoviePatchRequest(
        String title, Long primaryGenreId, List<Long> genreIds, LocalDate releaseDate, Integer durationMinutes,
        String version, String castText, String director, String studio, String synopsis, String posterUrl,
        String backdropUrl, String ageRating, String status) {

    private static final Set<String> ALLOWED = Set.of("title", "primaryGenreId", "genreIds", "releaseDate",
            "durationMinutes", "version", "castText", "director", "studio", "synopsis", "posterUrl",
            "backdropUrl", "ageRating", "status");

    public static AdminMoviePatchRequest from(JsonNode body) {
        if (body == null || !body.isObject()) throw BusinessException.badRequest("Nội dung yêu cầu không hợp lệ");
        for (String field : body.propertyNames()) {
            if (!ALLOWED.contains(field)) throw BusinessException.badRequest("Trường không hỗ trợ: " + field);
            if (body.get(field).isNull()) throw BusinessException.badRequest(field + " không được null");
        }
        return new AdminMoviePatchRequest(text(body, "title"), longValue(body, "primaryGenreId"),
                genreIds(body), date(body, "releaseDate"), integer(body, "durationMinutes"), text(body, "version"),
                text(body, "castText"), text(body, "director"), text(body, "studio"), text(body, "synopsis"),
                text(body, "posterUrl"), text(body, "backdropUrl"), text(body, "ageRating"), text(body, "status"));
    }

    private static String text(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null) return null;
        if (!value.isTextual()) throw BusinessException.badRequest(field + " phải là chuỗi");
        return value.textValue();
    }

    private static Long longValue(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null) return null;
        if (!value.canConvertToLong()) throw BusinessException.badRequest(field + " phải là số nguyên");
        return value.longValue();
    }

    private static Integer integer(JsonNode body, String field) {
        Long value = longValue(body, field);
        if (value == null) return null;
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            throw BusinessException.badRequest(field + " vượt phạm vi hợp lệ");
        }
        return value.intValue();
    }

    private static LocalDate date(JsonNode body, String field) {
        String value = text(body, field);
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw BusinessException.badRequest(field + " phải theo định dạng YYYY-MM-DD");
        }
    }

    private static List<Long> genreIds(JsonNode body) {
        JsonNode value = body.get("genreIds");
        if (value == null) return null;
        if (!value.isArray()) throw BusinessException.badRequest("genreIds phải là mảng");
        List<Long> ids = new ArrayList<>();
        for (JsonNode item : value) {
            if (!item.canConvertToLong()) throw BusinessException.badRequest("genreIds phải chứa số nguyên");
            ids.add(item.longValue());
        }
        return List.copyOf(ids);
    }
}
