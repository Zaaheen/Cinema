package com.cinema.controller;

import com.cinema.common.ApiResponse;
import com.cinema.common.PageResponse;
import com.cinema.dto.price.SeatPriceAdminResponse;
import com.cinema.dto.price.SeatPriceRequest;
import com.cinema.dto.price.SeatPriceUpdateRequest;
import com.cinema.service.SeatPriceAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/v1/admin/seat-prices")
@Tag(name = "Quản trị bảng giá", description = "Quy tắc giá vé; chỉ ADMIN được truy cập")
public class SeatPriceAdminController {
    private final SeatPriceAdminService seatPriceAdminService;

    public SeatPriceAdminController(SeatPriceAdminService seatPriceAdminService) {
        this.seatPriceAdminService = seatPriceAdminService;
    }

    @GetMapping
    @Operation(summary = "Danh sách bảng giá đã được phê duyệt")
    public ApiResponse<PageResponse<SeatPriceAdminResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String seatType,
            @RequestParam(required = false) String movieVersion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveOn,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(seatPriceAdminService.list(status, seatType, movieVersion, effectiveOn, page, size));
    }

    @PostMapping
    @Operation(summary = "Tạo quy tắc giá ở trạng thái DRAFT")
    public ResponseEntity<ApiResponse<SeatPriceAdminResponse>> create(
            @Valid @RequestBody SeatPriceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Tạo bản nháp bảng giá thành công",
                seatPriceAdminService.createDraft(request)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Cập nhật, kích hoạt, ngừng áp dụng hoặc tạo successor revision")
    public ApiResponse<SeatPriceAdminResponse> update(@PathVariable Long id,
            @Valid @RequestBody SeatPriceUpdateRequest request) {
        return ApiResponse.ok("Cập nhật bảng giá thành công", seatPriceAdminService.update(id, request));
    }
}
