package com.cinema.dto.admin.movie;

/** Deliberately excludes provider-specific identifiers and upload metadata. */
public record AdminMoviePosterResponse(Long movieId, String posterUrl) {
}
