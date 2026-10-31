package com.cinema.exception;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        String message,
        List<FieldErrorItem> errors,
        Instant timestamp) {

    public record FieldErrorItem(String field, String message) {
    }

    public static ApiErrorResponse of(String message) {
        return new ApiErrorResponse(false, message, List.of(), Instant.now());
    }

    public static ApiErrorResponse of(String message, List<FieldErrorItem> errors) {
        return new ApiErrorResponse(false, message, errors, Instant.now());
    }
}
