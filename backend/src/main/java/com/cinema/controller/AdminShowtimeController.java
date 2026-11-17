package com.cinema.controller;

import com.cinema.common.ApiResponse;
import com.cinema.dto.catalog.AdminShowtimeResponse;
import com.cinema.dto.catalog.CreateShowtimesRequest;
import com.cinema.dto.catalog.RoomResponse;
import com.cinema.dto.catalog.ShowtimeSlotRequest;
import com.cinema.service.AdminShowtimeService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/admin")
public class AdminShowtimeController {

    private final AdminShowtimeService adminShowtimeService;

    public AdminShowtimeController(AdminShowtimeService adminShowtimeService) {
        this.adminShowtimeService = adminShowtimeService;
    }

    @GetMapping("/rooms")
    @Operation(summary = "Danh sách phòng chiếu đang hoạt động")
    public ApiResponse<List<RoomResponse>> rooms() {
        return ApiResponse.ok(adminShowtimeService.rooms());
    }

    @GetMapping("/showtimes")
    @Operation(summary = "Lịch chiếu quản trị theo ngày")
    public ApiResponse<List<AdminShowtimeResponse>> showtimes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(adminShowtimeService.showtimes(date));
    }

    @PostMapping("/showtimes")
    @Operation(summary = "Tạo nhiều suất chiếu cho một phim")
    public ResponseEntity<ApiResponse<List<AdminShowtimeResponse>>> createShowtimes(
            @Valid @RequestBody CreateShowtimesRequest request) {
        List<AdminShowtimeResponse> created = adminShowtimeService.createShowtimes(
                request.movieId(), request.showtimes());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo lịch chiếu thành công", created));
    }

    @PatchMapping("/showtimes/{showtimeId}")
    @Operation(summary = "Đổi giờ hoặc phòng của suất chiếu chưa có booking")
    public ApiResponse<AdminShowtimeResponse> updateShowtime(
            @PathVariable Long showtimeId,
            @Valid @RequestBody ShowtimeSlotRequest request) {
        return ApiResponse.ok("Cập nhật suất chiếu thành công",
                adminShowtimeService.updateShowtime(showtimeId, request));
    }

    @DeleteMapping("/showtimes/{showtimeId}")
    @Operation(summary = "Hủy suất chiếu chưa có booking; giữ lại lịch sử")
    public ResponseEntity<Void> cancelShowtime(@PathVariable Long showtimeId) {
        adminShowtimeService.cancelShowtime(showtimeId);
        return ResponseEntity.noContent().build();
    }
}
