package com.cinema.dto.catalog;

import com.cinema.entity.Movie;
import java.time.LocalDate;

import java.util.List;

public record MovieDetailResponse(Long id, String title, String genre, List<GenreResponse> genres, LocalDate releaseDate,
        int durationMinutes, String version, String castText, String director, String studio, String synopsis,
        String trailerUrl, String posterUrl, String status) {
    public static MovieDetailResponse from(Movie movie) {
        List<GenreResponse> genres = GenreResponse.fromAll(movie.getGenres());
        return new MovieDetailResponse(movie.getId(), movie.getTitle(), GenreResponse.displayNames(genres), genres, movie.getReleaseDate(),
                movie.getDurationMinutes(), movie.getVersion(), movie.getCastText(), movie.getDirector(),
                movie.getStudio(), movie.getSynopsis(), movie.getTrailerUrl(), movie.getPosterUrl(), movie.getStatus());
    }
}
