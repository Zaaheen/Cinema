package com.cinema.dto.catalog;

import java.util.List;

public record HomeResponse(List<BannerResponse> banners, List<MovieSummaryResponse> nowShowing, List<MovieSummaryResponse> comingSoon,
        List<HomeShowtimeResponse> showtimes, List<PromotionResponse> promotions) {}
