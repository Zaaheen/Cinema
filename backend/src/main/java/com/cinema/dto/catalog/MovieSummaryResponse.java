package com.cinema.dto.catalog;

import com.cinema.entity.Movie;
import java.time.LocalDate;
import java.util.List;

public record MovieSummaryResponse(Long id, String title, String genre, List<GenreResponse> genres, LocalDate releaseDate,
        int durationMinutes, String version, String posterUrl, String status) {
    public static MovieSummaryResponse from(Movie movie) {
        List<GenreResponse> genres = GenreResponse.fromAll(movie.getGenres());
        return new MovieSummaryResponse(movie.getId(), movie.getTitle(), GenreResponse.displayNames(genres), genres, movie.getReleaseDate(),
                movie.getDurationMinutes(), movie.getVersion(), movie.getPosterUrl(), movie.getStatus());
    }
}
