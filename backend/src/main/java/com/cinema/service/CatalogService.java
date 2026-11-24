package com.cinema.service;

import com.cinema.common.PageResponse;
import com.cinema.dto.catalog.HomeResponse;
import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.MovieSummaryResponse;
import com.cinema.dto.catalog.PromotionResponse;
import com.cinema.dto.catalog.ShowtimeResponse;
import com.cinema.dto.catalog.SeatMapResponse;
import com.cinema.dto.catalog.TicketPriceResponse;
import java.time.LocalDate;
import java.util.List;

public interface CatalogService {
    PageResponse<MovieSummaryResponse> movies(String status, String query, int page, int size);
    MovieDetailResponse movie(Long movieId);
    List<PromotionResponse> promotions();
    PromotionResponse promotion(Long promotionId);
    List<TicketPriceResponse> ticketPrices(LocalDate effectiveOn);
    List<ShowtimeResponse> showtimes(Long movieId, LocalDate date);
    SeatMapResponse seatMap(Long showtimeId, String memberUsername);
    HomeResponse home();
}
