package com.cinema.service.impl;

import com.cinema.dto.catalog.CreateMovieRequest;
import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.UpdateMovieRequest;
import com.cinema.entity.Movie;
import com.cinema.entity.Genre;
import com.cinema.exception.BusinessException;
import com.cinema.repository.GenreRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.service.AdminShowtimeService;
import com.cinema.service.AdminMovieService;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMovieServiceImpl implements AdminMovieService {

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final AdminShowtimeService adminShowtimeService;

    public AdminMovieServiceImpl(
            MovieRepository movieRepository,
            GenreRepository genreRepository,
            AdminShowtimeService adminShowtimeService) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.adminShowtimeService = adminShowtimeService;
    }

    @Override
    @Transactional
    public MovieDetailResponse createMovie(CreateMovieRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.title().trim());
        movie.setGenres(resolveGenres(request.genreIds(), Set.of()));
        movie.setReleaseDate(request.releaseDate());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setVersion(request.version().trim());
        movie.setCastText(request.castText());
        movie.setDirector(request.director());
        movie.setStudio(request.studio());
        movie.setSynopsis(request.synopsis());
        movie.setTrailerUrl(request.trailerUrl());
        movie.setPosterUrl(request.posterUrl());

        // Giả định minh họa: cần nhóm xác nhận trạng thái ban đầu.
        movie.setStatus("COMING_SOON");

        Movie savedMovie = movieRepository.saveAndFlush(movie);
        adminShowtimeService.createForMovie(savedMovie, request.showtimes());
        return MovieDetailResponse.from(savedMovie);
    }

    @Override
    @Transactional
    public MovieDetailResponse updateMovie(Long movieId, UpdateMovieRequest request) {
        Movie movie = movieRepository.findByIdForUpdate(movieId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phim"));

        movie.setTitle(request.title().trim());
        Set<Long> existingLegacyGenreIds = movie.getGenres().stream()
                .filter(genre -> !genre.isSelectable())
                .map(Genre::getId)
                .collect(java.util.stream.Collectors.toSet());
        movie.setGenres(resolveGenres(request.genreIds(), existingLegacyGenreIds));
        movie.setReleaseDate(request.releaseDate());
        adminShowtimeService.updateMovieDuration(movie, request.durationMinutes());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setVersion(request.version().trim());
        movie.setCastText(request.castText());
        movie.setDirector(request.director());
        movie.setStudio(request.studio());
        movie.setSynopsis(request.synopsis());
        movie.setTrailerUrl(request.trailerUrl());
        movie.setPosterUrl(request.posterUrl());

        // Giữ nguyên status; quy tắc đổi trạng thái chưa được nhóm phê duyệt.
        return MovieDetailResponse.from(movieRepository.save(movie));
    }

    private Set<Genre> resolveGenres(List<Long> requestedIds, Set<Long> existingLegacyGenreIds) {
        if (requestedIds == null || requestedIds.isEmpty()) return new LinkedHashSet<>();
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw BusinessException.badRequest("Không được chọn trùng thể loại");
        }
        List<Genre> genres = genreRepository.findAllById(requestedIds);
        if (genres.size() != requestedIds.size()) {
            throw BusinessException.badRequest("Có thể loại không tồn tại");
        }
        if (genres.stream().anyMatch(genre -> !genre.isSelectable()
                && !existingLegacyGenreIds.contains(genre.getId()))) {
            throw BusinessException.badRequest("Thể loại này chỉ được giữ cho dữ liệu phim cũ");
        }
        return new LinkedHashSet<>(genres);
    }
}
