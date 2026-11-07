package com.cinema.dto.catalog;

import com.cinema.entity.Movie;
import java.time.LocalDate;

public record MovieSummaryResponse(Long id, String title, String genre, LocalDate releaseDate,
        int durationMinutes, String version, String posterUrl, String status) {
    public static MovieSummaryResponse from(Movie movie) {
        return new MovieSummaryResponse(movie.getId(), movie.getTitle(), movie.getGenre(), movie.getReleaseDate(),
                movie.getDurationMinutes(), movie.getVersion(), movie.getPosterUrl(), movie.getStatus());
    }
}
