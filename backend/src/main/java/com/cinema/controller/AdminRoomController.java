package com.cinema.controller;

import com.cinema.common.ApiResponse;
import com.cinema.dto.admin.showtime.AdminRoomResponse;
import com.cinema.service.AdminShowtimeService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/rooms")
public class AdminRoomController {
    private final AdminShowtimeService adminShowtimeService;

    public AdminRoomController(AdminShowtimeService adminShowtimeService) {
        this.adminShowtimeService = adminShowtimeService;
    }

    @GetMapping
    @Operation(summary = "Liệt kê phòng chiếu cho quản trị lịch chiếu")
    public ApiResponse<List<AdminRoomResponse>> list() {
        return ApiResponse.ok(adminShowtimeService.listRooms());
    }
}
