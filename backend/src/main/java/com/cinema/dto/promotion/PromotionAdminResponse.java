
package com.cinema.dto.promotion;

import com.cinema.entity.Promotion;
import java.math.BigDecimal;
import java.time.Instant;

public record PromotionAdminResponse(
        Long id,
        String title,
        String description,
        String discountType,
        BigDecimal discountValue,
        Instant startsAt,
        Instant endsAt,
        String status) {

    public static PromotionAdminResponse from(Promotion promotion) {
        return new PromotionAdminResponse(
                promotion.getId(),
                promotion.getTitle(),
                promotion.getDescription(),
                promotion.getDiscountType(),
                promotion.getDiscountValue(),
                promotion.getStartsAt(),
                promotion.getEndsAt(),
                promotion.getStatus()
        );
    }
}
