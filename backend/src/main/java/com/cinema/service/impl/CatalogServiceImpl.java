
package com.cinema.service.impl;
import java.time.Duration;
import java.util.Optional;
import com.cinema.common.PageResponse;
import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.MovieSummaryResponse;
import com.cinema.dto.catalog.PromotionResponse;
import com.cinema.dto.catalog.ShowtimeResponse;
import com.cinema.dto.catalog.TicketPriceResponse;
import com.cinema.entity.Movie;
import com.cinema.exception.BusinessException;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.PromotionRepository;
import com.cinema.repository.SeatPriceRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.service.CatalogService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private static final ZoneId CINEMA_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private final MovieRepository movieRepository;
    private final PromotionRepository promotionRepository;
    private final SeatPriceRepository seatPriceRepository;
    private final ShowtimeRepository showtimeRepository;

    public CatalogServiceImpl(
            MovieRepository movieRepository,
            PromotionRepository promotionRepository,
            SeatPriceRepository seatPriceRepository,
            ShowtimeRepository showtimeRepository
    ) {
        this.movieRepository = movieRepository;
        this.promotionRepository = promotionRepository;
        this.seatPriceRepository = seatPriceRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public PageResponse<MovieSummaryResponse> movies(
            String status, String query, int page, int size
    ) {
        String normalizedStatus = StringUtils.hasText(status)
                ? status.trim().toUpperCase() : null;

        String normalizedQuery = StringUtils.hasText(query)
                ? query.trim() : null;

        Specification<Movie> specification = (root, criteriaQuery, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates =
                    new ArrayList<>();

            predicates.add(root.get("status").in(
                    "NOW_SHOWING", "COMING_SOON"
            ));

            if (normalizedStatus != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"), normalizedStatus
                ));
            }

            if (normalizedQuery != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        "%" + normalizedQuery.toLowerCase() + "%"
                ));
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            jakarta.persistence.criteria.Predicate[]::new
                    )
            );
        };

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "releaseDate")
                        .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        return PageResponse.from(
                movieRepository.findAll(specification, pageRequest)
                        .map(MovieSummaryResponse::from)
        );
    }

    @Override
    public MovieDetailResponse movie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() ->
                        BusinessException.notFound("Không tìm thấy phim")
                );

        return MovieDetailResponse.from(movie);
    }

    @Override
    public List<PromotionResponse> promotions() {
        return promotionRepository.findPublished(Instant.now())
                .stream()
                .map(PromotionResponse::from)
                .toList();
    }

    @Override
    public List<TicketPriceResponse> ticketPrices() {
        return seatPriceRepository.findEffectiveOn(
                        LocalDate.now(CINEMA_ZONE)
                )
                .stream()
                .map(TicketPriceResponse::from)
                .toList();
    }

    @Override
    public List<ShowtimeResponse> showtimes(
            Long movieId, LocalDate date
    ) {
        // 1. Chỉ cho phép xem lịch chiếu của phim được công khai.
        Optional<Movie> movie = movieRepository.findByIdAndStatusIn(
                movieId,
                List.of("NOW_SHOWING", "COMING_SOON")
        );

        if (movie.isEmpty()) {
            throw BusinessException.notFound(
                    "Không tìm thấy phim hoặc phim chưa được công khai"
            );
        }

        // 2. Lấy ngày hiện tại theo múi giờ Việt Nam.
        LocalDate today = LocalDate.now(CINEMA_ZONE);

        // 3. Ngày đã qua thì không có lịch chiếu được công bố.
        if (date.isBefore(today)) {
            return List.of();
        }

        // 4. Lấy thời điểm hiện tại.
        Instant now = Instant.now();

        // 5. Tính mốc đóng bán online: trước giờ chiếu 30 phút.
        Instant salesCloseThreshold = now.plus(
                Duration.ofMinutes(30)
        );

        // 6. Xác định thời điểm bắt đầu ngày được chọn tại Việt Nam.
        Instant startOfDay = date.atStartOfDay(CINEMA_ZONE)
                .toInstant();

        // 7. Xác định thời điểm bắt đầu ngày tiếp theo tại Việt Nam.
        Instant startOfNextDay = date.plusDays(1)
                .atStartOfDay(CINEMA_ZONE)
                .toInstant();

        // 8. Truy vấn các suất chiếu còn mở bán online.
        return showtimeRepository
                .findScheduledShowtimesByMovieAndDate(
                        movieId,
                        startOfDay,
                        startOfNextDay,
                        salesCloseThreshold
                )
                .stream()
                .map(ShowtimeResponse::from)
                .toList();
    }
}
