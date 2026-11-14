
package com.cinema.service;

import com.cinema.dto.promotion.PromotionAdminResponse;
import com.cinema.dto.promotion.PromotionRequest;

import java.util.List;

public interface PromotionService {

    List<PromotionAdminResponse> getAllPromotions();

    PromotionAdminResponse getPromotionById(Long id);

    PromotionAdminResponse createPromotion(PromotionRequest request);

    PromotionAdminResponse updatePromotion(Long id, PromotionRequest request);
}
