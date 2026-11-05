package com.cinema.dto.catalog;

import com.cinema.entity.Promotion;
import java.math.BigDecimal;
import java.time.Instant;

public record PromotionResponse(Long id, String title, String description, String discountType,
        BigDecimal discountValue, Instant startsAt, Instant endsAt) {
    public static PromotionResponse from(Promotion promotion) {
        return new PromotionResponse(promotion.getId(), promotion.getTitle(), promotion.getDescription(),
                promotion.getDiscountType(), promotion.getDiscountValue(), promotion.getStartsAt(), promotion.getEndsAt());
    }
}
