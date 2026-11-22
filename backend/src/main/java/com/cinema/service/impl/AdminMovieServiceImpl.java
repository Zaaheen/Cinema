package com.cinema.service.impl;

import com.cinema.dto.admin.movie.AdminMoviePatchRequest;
import com.cinema.dto.admin.movie.AdminMoviePosterResponse;
import com.cinema.dto.admin.movie.AdminMovieRequest;
import com.cinema.dto.admin.movie.AdminMovieResponse;
import com.cinema.dto.admin.movie.AdminMovieTrailerPatchRequest;
import com.cinema.dto.admin.movie.AdminGenreResponse;
import com.cinema.common.PageResponse;
import com.cinema.entity.Genre;
import com.cinema.entity.Movie;
import com.cinema.exception.BusinessException;
import com.cinema.repository.GenreRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.service.AdminMovieService;
import com.cinema.service.CloudinaryPosterService;
import com.cinema.service.YoutubeTrailerUrlValidator;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminMovieServiceImpl implements AdminMovieService {
    private static final Set<String> MOVIE_STATUSES = Set.of("COMING_SOON", "NOW_SHOWING", "STOPPED");
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final ShowtimeRepository showtimeRepository;
    private final CloudinaryPosterService cloudinaryPosterService;
    private final YoutubeTrailerUrlValidator youtubeTrailerUrlValidator;
    private final TransactionTemplate transactionTemplate;

    public AdminMovieServiceImpl(MovieRepository movieRepository, GenreRepository genreRepository,
            ShowtimeRepository showtimeRepository, CloudinaryPosterService cloudinaryPosterService,
            YoutubeTrailerUrlValidator youtubeTrailerUrlValidator, TransactionTemplate transactionTemplate) {
        this.movieRepository = movieRepository;
        this.genreRepository = genreRepository;
        this.showtimeRepository = showtimeRepository;
        this.cloudinaryPosterService = cloudinaryPosterService;
        this.youtubeTrailerUrlValidator = youtubeTrailerUrlValidator;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminMovieResponse> list(String query, String status, int page, int size) {
        String normalized = query == null ? "" : query.trim();
        Specification<Movie> specification = (root, criteriaQuery, builder) -> builder.conjunction();
        if (!normalized.isEmpty()) specification = specification.and((root, criteriaQuery, builder) -> builder.like(builder.lower(root.get("title")), "%" + normalized.toLowerCase() + "%"));
        if (status != null && !status.isBlank()) {
            if (!MOVIE_STATUSES.contains(status.trim())) throw BusinessException.badRequest("status không hợp lệ");
            specification = specification.and((root, criteriaQuery, builder) -> builder.equal(root.get("status"), status.trim()));
        }
        var movies = movieRepository.findAll(specification, PageRequest.of(page, size, Sort.by("title").ascending().and(Sort.by("id").ascending())));
        return PageResponse.from(movies.map(AdminMovieResponse::from));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminMovieResponse get(Long movieId) {
        return movieRepository.findWithGenresById(movieId).map(AdminMovieResponse::from)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phim"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminGenreResponse> genres() { return genreRepository.findBySelectableTrueOrderByNameAscIdAsc().stream().map(AdminGenreResponse::from).toList(); }

    @Override
    @Transactional
    public AdminMovieResponse create(AdminMovieRequest request) {
        GenreSelection selection = resolveSelectableGenres(request.genreIds(), request.primaryGenreId());
        Movie movie = new Movie();
        movie.setTitle(trimRequired(request.title(), "title", 255));
        movie.setGenre(selection.primary().getName());
        movie.setGenres(selection.genres());
        movie.setReleaseDate(request.releaseDate());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setVersion(trimRequired(request.version(), "version", 50));
        movie.setCastText(request.castText());
        movie.setDirector(boundedText(request.director(), "director", 255));
        movie.setStudio(boundedText(request.studio(), "studio", 255));
        movie.setSynopsis(request.synopsis());
        movie.setPosterUrl(validHttpsUrl(request.posterUrl(), "posterUrl"));
        movie.setBackdropUrl(validHttpsUrl(request.backdropUrl(), "backdropUrl"));
        movie.setAgeRating(boundedText(request.ageRating(), "ageRating", 10));
        movie.setStatus("COMING_SOON");
        movie.setTrailerUrl(null);
        movie.setSourceKey(null);
        return AdminMovieResponse.from(movieRepository.saveAndFlush(movie));
    }

    @Override
    @Transactional
    public AdminMovieResponse update(Long movieId, AdminMoviePatchRequest request) {
        Movie movie = movieRepository.findByIdForUpdate(movieId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phim"));
        boolean hasGenreIds = request.genreIds() != null;
        boolean hasPrimaryGenreId = request.primaryGenreId() != null;
        if (hasGenreIds != hasPrimaryGenreId) {
            throw BusinessException.badRequest("genreIds và primaryGenreId phải được gửi cùng nhau");
        }
        if (hasGenreIds) {
            GenreSelection selection = resolveSelectableGenres(request.genreIds(), request.primaryGenreId());
            Set<Genre> retainedLegacyGenres = movie.getGenres().stream()
                    .filter(genre -> !genre.isSelectable())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            retainedLegacyGenres.addAll(selection.genres());
            movie.setGenres(retainedLegacyGenres);
            movie.setGenre(selection.primary().getName());
        }
        applyIfPresent(request.title(), value -> movie.setTitle(trimRequired(value, "title", 255)));
        applyIfPresent(request.releaseDate(), movie::setReleaseDate);
        if (request.durationMinutes() != null && request.durationMinutes() <= 0) {
            throw BusinessException.badRequest("durationMinutes phải lớn hơn 0");
        }
        if (request.durationMinutes() != null && request.durationMinutes() != movie.getDurationMinutes()) {
            if (showtimeRepository.existsByMovieIdAndStatusNot(movieId, "CANCELLED")) {
                throw BusinessException.badRequest("Không thể đổi thời lượng khi phim đã có suất chiếu chưa hủy");
            }
            movie.setDurationMinutes(request.durationMinutes());
        }
        applyIfPresent(request.version(), value -> movie.setVersion(trimRequired(value, "version", 50)));
        applyIfPresent(request.castText(), movie::setCastText);
        applyIfPresent(request.director(), value -> movie.setDirector(boundedText(value, "director", 255)));
        applyIfPresent(request.studio(), value -> movie.setStudio(boundedText(value, "studio", 255)));
        applyIfPresent(request.synopsis(), movie::setSynopsis);
        applyIfPresent(request.posterUrl(), value -> movie.setPosterUrl(validHttpsUrl(value, "posterUrl")));
        applyIfPresent(request.backdropUrl(), value -> movie.setBackdropUrl(validHttpsUrl(value, "backdropUrl")));
        applyIfPresent(request.ageRating(), value -> movie.setAgeRating(boundedText(value, "ageRating", 10)));
        return AdminMovieResponse.from(movieRepository.saveAndFlush(movie));
    }

    @Override
    public AdminMoviePosterResponse uploadPoster(Long movieId, MultipartFile file) {
        if (!movieRepository.existsById(movieId)) {
            throw BusinessException.notFound("Không tìm thấy phim");
        }
        String posterUrl = cloudinaryPosterService.uploadPng(file);
        return transactionTemplate.execute(status -> {
            Movie movie = movieRepository.findByIdForUpdate(movieId)
                    .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phim"));
            movie.setPosterUrl(posterUrl);
            Movie saved = movieRepository.saveAndFlush(movie);
            return new AdminMoviePosterResponse(saved.getId(), saved.getPosterUrl());
        });
    }

    @Override
    @Transactional
    public AdminMovieResponse updateTrailer(Long movieId, AdminMovieTrailerPatchRequest request) {
        Movie movie = movieRepository.findByIdForUpdate(movieId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phim"));
        movie.setTrailerUrl(request.trailerUrl() == null ? null : youtubeTrailerUrlValidator.normalize(request.trailerUrl()));
        return AdminMovieResponse.from(movieRepository.saveAndFlush(movie));
    }

    private GenreSelection resolveSelectableGenres(List<Long> requestedIds, Long primaryGenreId) {
        if (requestedIds == null || requestedIds.isEmpty() || requestedIds.size() > 6) {
            throw BusinessException.badRequest("Phải chọn ít nhất một thể loại");
        }
        if (primaryGenreId == null || primaryGenreId <= 0 || requestedIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw BusinessException.badRequest("genreIds và primaryGenreId phải là số dương");
        }
        if (new HashSet<>(requestedIds).size() != requestedIds.size()) {
            throw BusinessException.badRequest("Không được chọn trùng thể loại");
        }
        List<Genre> found = genreRepository.findAllById(requestedIds);
        if (found.size() != requestedIds.size()) {
            throw BusinessException.badRequest("Có thể loại không tồn tại");
        }
        if (found.stream().anyMatch(genre -> !genre.isSelectable())) {
            throw BusinessException.badRequest("Chỉ được chọn thể loại chuẩn đang khả dụng");
        }
        Map<Long, Genre> byId = found.stream().collect(Collectors.toMap(Genre::getId, genre -> genre));
        Genre primary = byId.get(primaryGenreId);
        if (primary == null) {
            throw BusinessException.badRequest("primaryGenreId phải thuộc genreIds");
        }
        return new GenreSelection(new LinkedHashSet<>(found), primary);
    }

    private String validHttpsUrl(String value, String field) {
        if (value == null) return null;
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw BusinessException.badRequest(field + " phải là URL HTTPS tuyệt đối");
            }
            return value;
        } catch (URISyntaxException exception) {
            throw BusinessException.badRequest(field + " phải là URL HTTPS tuyệt đối");
        }
    }

    private String trimRequired(String value, String field, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) throw BusinessException.badRequest(field + " không được để trống");
        if (trimmed.length() > maxLength) throw BusinessException.badRequest(field + " vượt quá độ dài cho phép");
        return trimmed;
    }

    private String boundedText(String value, String field, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw BusinessException.badRequest(field + " vượt quá độ dài cho phép");
        }
        return value;
    }

    private <T> void applyIfPresent(T value, Consumer<T> consumer) {
        if (value != null) consumer.accept(value);
    }

    private record GenreSelection(Set<Genre> genres, Genre primary) {
    }
}
