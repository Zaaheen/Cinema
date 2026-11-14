package com.cinema.service;

import com.cinema.common.PageResponse;
import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.MovieSummaryResponse;
import com.cinema.dto.catalog.PromotionResponse;
import com.cinema.dto.catalog.TicketPriceResponse;
import com.cinema.dto.catalog.ShowtimeResponse;
import java.time.LocalDate;
import java.util.List;

public interface CatalogService {
    PageResponse<MovieSummaryResponse> movies(String status, String query, int page, int size);
    MovieDetailResponse movie(Long movieId);
    List<PromotionResponse> promotions();
    List<TicketPriceResponse> ticketPrices();
    List<ShowtimeResponse> showtimes(Long movieId, LocalDate date);
}
