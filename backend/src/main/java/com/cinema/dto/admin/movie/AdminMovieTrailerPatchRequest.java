package com.cinema.dto.admin.movie;

import com.cinema.exception.BusinessException;
import tools.jackson.databind.JsonNode;

/** A separate request type keeps trailer changes outside Slice 2's generic PATCH contract. */
public record AdminMovieTrailerPatchRequest(String trailerUrl) {
    public static AdminMovieTrailerPatchRequest from(JsonNode body) {
        if (body == null || !body.isObject() || !body.has("trailerUrl") || body.size() != 1) {
            throw BusinessException.badRequest("Yêu cầu chỉ phải chứa trailerUrl");
        }
        JsonNode value = body.get("trailerUrl");
        if (!value.isNull() && !value.isTextual()) {
            throw BusinessException.badRequest("trailerUrl phải là chuỗi hoặc null");
        }
        return new AdminMovieTrailerPatchRequest(value.isNull() ? null : value.asText());
    }
}
