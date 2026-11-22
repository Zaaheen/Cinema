package com.cinema.dto.admin.movie;

import com.cinema.entity.Genre;
import com.cinema.entity.Movie;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record AdminMovieResponse(Long id, String title, String genre, Long primaryGenreId,
        List<AdminGenreResponse> genres, LocalDate releaseDate, int durationMinutes, String version,
        String castText, String director, String studio, String synopsis, String trailerUrl, String posterUrl,
        String backdropUrl, String ageRating, String sourceKey, String status) {

    public static AdminMovieResponse from(Movie movie) {
        List<Genre> orderedGenres = movie.getGenres().stream()
                .sorted(Comparator.comparing(Genre::getName).thenComparing(Genre::getId))
                .toList();
        Long primaryGenreId = orderedGenres.stream()
                .filter(Genre::isSelectable)
                .filter(genre -> genre.getName().equals(movie.getGenre()))
                .map(Genre::getId)
                .findFirst()
                .orElse(null);
        return new AdminMovieResponse(movie.getId(), movie.getTitle(), movie.getGenre(), primaryGenreId,
                orderedGenres.stream().map(AdminGenreResponse::from).toList(), movie.getReleaseDate(),
                movie.getDurationMinutes(), movie.getVersion(), movie.getCastText(), movie.getDirector(),
                movie.getStudio(), movie.getSynopsis(), movie.getTrailerUrl(), movie.getPosterUrl(),
                movie.getBackdropUrl(), movie.getAgeRating(), movie.getSourceKey(), movie.getStatus());
    }
}
