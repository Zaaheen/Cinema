package com.cinema.service.impl;

import com.cinema.common.PageResponse;
import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.MovieSummaryResponse;
import com.cinema.dto.catalog.PromotionResponse;
import com.cinema.dto.catalog.TicketPriceResponse;
import com.cinema.entity.Movie;
import com.cinema.exception.BusinessException;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.PromotionRepository;
import com.cinema.repository.SeatPriceRepository;
import com.cinema.service.CatalogService;
import java.time.Instant;
import java.time.LocalDate;
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
    private final MovieRepository movieRepository;
    private final PromotionRepository promotionRepository;
    private final SeatPriceRepository seatPriceRepository;

    public CatalogServiceImpl(MovieRepository movieRepository, PromotionRepository promotionRepository,
            SeatPriceRepository seatPriceRepository) {
        this.movieRepository = movieRepository;
        this.promotionRepository = promotionRepository;
        this.seatPriceRepository = seatPriceRepository;
    }

    @Override
    public PageResponse<MovieSummaryResponse> movies(String status, String query, int page, int size) {
        String normalizedStatus = StringUtils.hasText(status) ? status.trim().toUpperCase() : null;
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        Specification<Movie> specification = (root, criteriaQuery, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("status").in("NOW_SHOWING", "COMING_SOON"));
            if (normalizedStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), normalizedStatus));
            }
            if (normalizedQuery != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        "%" + normalizedQuery.toLowerCase() + "%"));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        PageRequest pageRequest = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "releaseDate").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(movieRepository.findAll(specification, pageRequest)
                .map(MovieSummaryResponse::from));
    }

    @Override
    public MovieDetailResponse movie(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> BusinessException.notFound("Không tìm thấy phim"));
        return MovieDetailResponse.from(movie);
    }

    @Override public List<PromotionResponse> promotions() {
        return promotionRepository.findPublished(Instant.now()).stream().map(PromotionResponse::from).toList();
    }

    @Override public List<TicketPriceResponse> ticketPrices() {
        return seatPriceRepository.findEffectiveOn(LocalDate.now()).stream().map(TicketPriceResponse::from).toList();
    }
}
