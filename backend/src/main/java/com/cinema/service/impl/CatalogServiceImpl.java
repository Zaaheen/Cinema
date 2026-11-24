
package com.cinema.service.impl;
import java.time.Duration;
import java.util.Optional;
import com.cinema.common.PageResponse;
import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.BannerResponse;
import com.cinema.dto.catalog.HomeResponse;
import com.cinema.dto.catalog.HomeShowtimeResponse;
import com.cinema.dto.catalog.MovieSummaryResponse;
import com.cinema.dto.catalog.PromotionResponse;
import com.cinema.dto.catalog.ShowtimeResponse;
import com.cinema.dto.catalog.SeatMapResponse;
import com.cinema.dto.catalog.TicketPriceResponse;
import com.cinema.entity.Movie;
import com.cinema.entity.BookingSeat;
import com.cinema.entity.Seat;
import com.cinema.entity.Showtime;
import com.cinema.exception.BusinessException;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.BannerRepository;
import com.cinema.repository.PromotionRepository;
import com.cinema.repository.SeatPriceRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.repository.SeatRepository;
import com.cinema.repository.BookingSeatRepository;
import com.cinema.service.CatalogService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final BannerRepository bannerRepository;
    private final PromotionRepository promotionRepository;
    private final SeatPriceRepository seatPriceRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final BookingSeatRepository bookingSeatRepository;

    public CatalogServiceImpl(
            MovieRepository movieRepository,
            BannerRepository bannerRepository,
            PromotionRepository promotionRepository,
            SeatPriceRepository seatPriceRepository,
            ShowtimeRepository showtimeRepository,
            SeatRepository seatRepository,
            BookingSeatRepository bookingSeatRepository
    ) {
        this.movieRepository = movieRepository;
        this.bannerRepository = bannerRepository;
        this.promotionRepository = promotionRepository;
        this.seatPriceRepository = seatPriceRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.bookingSeatRepository = bookingSeatRepository;
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
    public PromotionResponse promotion(Long promotionId) {
        return promotionRepository.findPublishedById(promotionId, Instant.now())
                .map(PromotionResponse::from)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy khuyến mãi"));
    }

    @Override
    public List<TicketPriceResponse> ticketPrices(LocalDate effectiveOn) {
        LocalDate requestedDate = effectiveOn == null ? LocalDate.now(CINEMA_ZONE) : effectiveOn;
        return seatPriceRepository.findPublicEffectiveOn(requestedDate)
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

    @Override
    public SeatMapResponse seatMap(Long showtimeId, String memberUsername) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy suất chiếu"));
        Instant now = Instant.now();
        boolean salesOpen = isSalesOpen(showtime, now);
        Map<Long, BookingSeat> activeSeats = bookingSeatRepository.findActiveForSeatMap(showtimeId, now)
                .stream()
                .collect(Collectors.toMap(item -> item.getSeat().getId(), item -> item));
        List<SeatMapResponse.SeatMapSeatResponse> seats = seatRepository
                .findByRoomIdOrderByRowLabelAscSeatNumberAsc(showtime.getRoom().getId())
                .stream()
                .map(seat -> seatResponse(seat, activeSeats.get(seat.getId()), memberUsername, salesOpen))
                .toList();
        return new SeatMapResponse(
                new SeatMapResponse.ShowtimeSeatMapResponse(
                        showtime.getId(), showtime.getMovie().getTitle(), showtime.getRoom().getName(),
                        showtime.getStartsAt(), showtime.getEndsAt(), showtime.getStatus()),
                now,
                salesOpen,
                seats);
    }

    private boolean isSalesOpen(Showtime showtime, Instant now) {
        return "SCHEDULED".equals(showtime.getStatus())
                && "ACTIVE".equals(showtime.getRoom().getStatus())
                && Set.of("NOW_SHOWING", "COMING_SOON").contains(showtime.getMovie().getStatus())
                && showtime.getStartsAt().isAfter(now.plus(Duration.ofMinutes(30)));
    }

    private SeatMapResponse.SeatMapSeatResponse seatResponse(
            Seat seat, BookingSeat bookingSeat, String memberUsername, boolean salesOpen
    ) {
        String availability;
        if (!salesOpen) {
            availability = "NOT_FOR_SALE";
        } else if (!"AVAILABLE".equals(seat.getStatus())) {
            availability = "UNAVAILABLE";
        } else if (bookingSeat == null) {
            availability = "AVAILABLE";
        } else if ("CONFIRMED".equals(bookingSeat.getStatus())) {
            availability = "SOLD";
        } else if (memberUsername != null
                && memberUsername.equals(bookingSeat.getBooking().getMember().getUser().getUsername())) {
            availability = "OWN_HELD";
        } else {
            availability = "HELD";
        }
        return new SeatMapResponse.SeatMapSeatResponse(
                seat.getId(), seat.getRowLabel(), seat.getSeatNumber(), seat.getSeatType(), availability);
    }

    @Override public HomeResponse home() {
        Instant now = Instant.now();
        return new HomeResponse(
                bannerRepository.findByActiveTrueOrderByDisplayOrderAscIdAsc().stream().map(BannerResponse::from).toList(),
                movieRepository.findByStatusOrderByReleaseDateDescIdDesc("NOW_SHOWING").stream().map(MovieSummaryResponse::from).toList(),
                movieRepository.findByStatusOrderByReleaseDateAscIdAsc("COMING_SOON").stream().map(MovieSummaryResponse::from).toList(),
                showtimeRepository.findBookableFrom(now).stream().map(HomeShowtimeResponse::from).toList(),
                promotionRepository.findPublished(now).stream().map(PromotionResponse::from).toList());
    }
}
