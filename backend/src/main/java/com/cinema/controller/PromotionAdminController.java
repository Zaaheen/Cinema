
package com.cinema.controller;

import com.cinema.common.ApiResponse;
import com.cinema.dto.promotion.PromotionAdminResponse;
import com.cinema.dto.promotion.PromotionRequest;
import com.cinema.service.PromotionService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/promotions")
public class PromotionAdminController {

    private final PromotionService promotionService;

    public PromotionAdminController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public ApiResponse<List<PromotionAdminResponse>> getAllPromotions() {
        return ApiResponse.ok(
                promotionService.getAllPromotions()
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<PromotionAdminResponse> getPromotionById(
            @PathVariable Long id) {

        return ApiResponse.ok(
                promotionService.getPromotionById(id)
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromotionAdminResponse>> createPromotion(
            @Valid @RequestBody PromotionRequest request) {

        PromotionAdminResponse response =
                promotionService.createPromotion(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        "Thêm khuyến mãi thành công",
                        response
                ));
    }

    @PatchMapping("/{id}")
    public ApiResponse<PromotionAdminResponse> updatePromotion(
            @PathVariable Long id,
            @Valid @RequestBody PromotionRequest request) {

        PromotionAdminResponse response =
                promotionService.updatePromotion(id, request);

        return ApiResponse.ok(
                "Cập nhật khuyến mãi thành công",
                response
        );
    }
}
