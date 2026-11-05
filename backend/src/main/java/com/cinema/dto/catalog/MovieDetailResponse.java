package com.cinema.dto.catalog;

import com.cinema.entity.Movie;
import java.time.LocalDate;

public record MovieDetailResponse(Long id, String title, String genre, LocalDate releaseDate,
        int durationMinutes, String version, String castText, String director, String studio, String synopsis,
        String trailerUrl, String posterUrl, String status) {
    public static MovieDetailResponse from(Movie movie) {
        return new MovieDetailResponse(movie.getId(), movie.getTitle(), movie.getGenre(), movie.getReleaseDate(),
                movie.getDurationMinutes(), movie.getVersion(), movie.getCastText(), movie.getDirector(),
                movie.getStudio(), movie.getSynopsis(), movie.getTrailerUrl(), movie.getPosterUrl(), movie.getStatus());
    }
}
