package com.cinema.service.impl;

import com.cinema.dto.catalog.CreateMovieRequest;
import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.UpdateMovieRequest;
import com.cinema.entity.Movie;
import com.cinema.exception.BusinessException;
import com.cinema.repository.MovieRepository;
import com.cinema.service.AdminMovieService;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMovieServiceImpl implements AdminMovieService {

    private static final Set<String> ALLOWED_GENRES = Set.of(
            "Hành động", "Hài kịch", "Kinh dị", "Khoa học viễn tưởng", "Tình cảm");

    private final MovieRepository movieRepository;

    public AdminMovieServiceImpl(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    @Transactional
    public MovieDetailResponse createMovie(CreateMovieRequest request) {
        Movie movie = new Movie();
        movie.setTitle(request.title().trim());
        movie.setGenre(request.genre());
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

        Movie savedMovie = movieRepository.save(movie);
        return MovieDetailResponse.from(savedMovie);
    }

    @Override
    @Transactional
    public MovieDetailResponse updateMovie(Long movieId, UpdateMovieRequest request) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phim"));

        if (request.genre() != null
                && !request.genre().equals(movie.getGenre())
                && !ALLOWED_GENRES.contains(request.genre())) {
            throw BusinessException.badRequest("Thể loại không nằm trong danh sách cho phép");
        }

        movie.setTitle(request.title().trim());
        movie.setGenre(request.genre());
        movie.setReleaseDate(request.releaseDate());
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
}
