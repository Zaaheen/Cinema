package com.cinema.controller;

import com.cinema.common.ApiResponse;
import com.cinema.dto.admin.showtime.AdminShowtimeCreateRequest;
import com.cinema.dto.admin.showtime.AdminShowtimePatchRequest;
import com.cinema.dto.admin.showtime.AdminShowtimeResponse;
import com.cinema.service.AdminShowtimeService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/v1/admin/showtimes")
public class AdminShowtimeController {
    private final AdminShowtimeService adminShowtimeService;
    public AdminShowtimeController(AdminShowtimeService adminShowtimeService) { this.adminShowtimeService = adminShowtimeService; }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminShowtimeResponse>> create(@RequestBody JsonNode body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Tạo suất chiếu thành công",
                adminShowtimeService.create(AdminShowtimeCreateRequest.from(body))));
    }

    @GetMapping
    public ApiResponse<List<AdminShowtimeResponse>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(adminShowtimeService.list(date));
    }

    @GetMapping("/{showtimeId}")
    public ApiResponse<AdminShowtimeResponse> get(@PathVariable Long showtimeId) {
        return ApiResponse.ok(adminShowtimeService.get(showtimeId));
    }

    @PatchMapping("/{showtimeId}")
    public ApiResponse<AdminShowtimeResponse> update(@PathVariable Long showtimeId, @RequestBody JsonNode body) {
        return ApiResponse.ok("Cập nhật suất chiếu thành công", adminShowtimeService.update(showtimeId, AdminShowtimePatchRequest.from(body)));
    }

    @PostMapping("/{showtimeId}/cancel")
    public ApiResponse<AdminShowtimeResponse> cancel(@PathVariable Long showtimeId) {
        return ApiResponse.ok("Hủy suất chiếu thành công", adminShowtimeService.cancel(showtimeId));
    }
}
