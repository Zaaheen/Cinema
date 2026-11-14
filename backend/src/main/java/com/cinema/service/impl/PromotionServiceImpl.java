
package com.cinema.service.impl;

import com.cinema.dto.promotion.PromotionAdminResponse;
import com.cinema.dto.promotion.PromotionRequest;
import com.cinema.entity.Promotion;
import com.cinema.exception.BusinessException;
import com.cinema.repository.PromotionRepository;
import com.cinema.service.PromotionService;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    public PromotionServiceImpl(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromotionAdminResponse> getAllPromotions() {
        return promotionRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "id")
                ).stream()
                .map(PromotionAdminResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionAdminResponse getPromotionById(Long id) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(
                        "Không tìm thấy khuyến mãi có mã: " + id
                ));

        return PromotionAdminResponse.from(promotion);
    }

    @Override
    public PromotionAdminResponse createPromotion(PromotionRequest request) {
        validatePromotion(request);

        Promotion promotion = new Promotion();
        applyRequest(promotion, request);

        if (request.status() == null || request.status().isBlank()) {
            promotion.setStatus("ACTIVE");
        }

        Promotion savedPromotion = promotionRepository.save(promotion);

        return PromotionAdminResponse.from(savedPromotion);
    }

    @Override
    public PromotionAdminResponse updatePromotion(
            Long id,
            PromotionRequest request
    ) {
        validatePromotion(request);

        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound(
                        "Không tìm thấy khuyến mãi có mã: " + id
                ));

        applyRequest(promotion, request);

        if (request.status() != null && !request.status().isBlank()) {
            promotion.setStatus(request.status().trim().toUpperCase());
        }

        Promotion updatedPromotion = promotionRepository.save(promotion);

        return PromotionAdminResponse.from(updatedPromotion);
    }

    private void applyRequest(
            Promotion promotion,
            PromotionRequest request
    ) {
        promotion.setTitle(request.title().trim());
        promotion.setDescription(request.description());
        promotion.setDiscountType(
                request.discountType().trim().toUpperCase()
        );
        promotion.setDiscountValue(request.discountValue());
        promotion.setStartsAt(request.startsAt());
        promotion.setEndsAt(request.endsAt());

        if (request.status() != null && !request.status().isBlank()) {
            promotion.setStatus(request.status().trim().toUpperCase());
        }
    }

    private void validatePromotion(PromotionRequest request) {
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw BusinessException.badRequest(
                    "Thời gian kết thúc phải sau thời gian bắt đầu"
            );
        }

        if ("PERCENT".equalsIgnoreCase(request.discountType())
                && request.discountValue().compareTo(
                new BigDecimal("100")
        ) > 0) {
            throw BusinessException.badRequest(
                    "Giảm giá theo phần trăm không được vượt quá 100%"
            );
        }
    }
}
