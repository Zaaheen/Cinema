package com.cinema.controller;

import com.cinema.common.ApiResponse;
import com.cinema.common.PageResponse;
import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.MovieSummaryResponse;
import com.cinema.dto.catalog.PromotionResponse;
import com.cinema.dto.catalog.TicketPriceResponse;
import com.cinema.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import com.cinema.dto.catalog.ShowtimeResponse;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1")
@Tag(name = "Dữ liệu công khai", description = "Phim, khuyến mãi và giá vé")
public class CatalogController {
    private final CatalogService catalogService;
    public CatalogController(CatalogService catalogService) { this.catalogService = catalogService; }

    @GetMapping("/movies") @Operation(summary = "Danh sách và tìm kiếm phim")
    public ApiResponse<PageResponse<MovieSummaryResponse>> movies(
            @RequestParam(required = false) String status, @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(catalogService.movies(status, query, page, size));
    }

    @GetMapping("/movies/{movieId}") @Operation(summary = "Chi tiết phim")
    public ApiResponse<MovieDetailResponse> movie(@PathVariable Long movieId) { return ApiResponse.ok(catalogService.movie(movieId)); }

    @GetMapping("/promotions") @Operation(summary = "Khuyến mãi đang công bố")
    public ApiResponse<List<PromotionResponse>> promotions() { return ApiResponse.ok(catalogService.promotions()); }

    @GetMapping("/ticket-prices") @Operation(summary = "Giá vé đang áp dụng")
    public ApiResponse<List<TicketPriceResponse>> ticketPrices() { return ApiResponse.ok(catalogService.ticketPrices()); }

    @GetMapping("/movies/{movieId}/showtimes")
    @Operation(summary = "Xem lịch chiếu của phim theo ngày")
    public ApiResponse<List<ShowtimeResponse>> showtimes(
            @PathVariable Long movieId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ApiResponse.ok(
                catalogService.showtimes(movieId, date)
        );
    }
}
