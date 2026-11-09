
package com.cinema.dto.promotion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionRequest(

        @NotBlank(message = "Tên khuyến mãi không được để trống")
        @Size(max = 255, message = "Tên khuyến mãi tối đa 255 ký tự")
        String title,

        String description,

        @NotBlank(message = "Loại giảm giá không được để trống")
        @Pattern(
                regexp = "(?i)(PERCENT|FIXED)",
                message = "Loại giảm giá chỉ nhận PERCENT hoặc FIXED"
        )
        String discountType,

        @NotNull(message = "Giá trị giảm giá không được để trống")
        @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
        BigDecimal discountValue,

        @NotNull(message = "Thời gian bắt đầu không được để trống")
        Instant startsAt,

        @NotNull(message = "Thời gian kết thúc không được để trống")
        Instant endsAt,

        @Pattern(
                regexp = "(?i)(ACTIVE|INACTIVE|EXPIRED)",
                message = "Trạng thái không hợp lệ"
        )
        String status

) {
}
