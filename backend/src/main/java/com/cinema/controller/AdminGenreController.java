package com.cinema.controller;
import com.cinema.common.ApiResponse;
import com.cinema.dto.admin.movie.AdminGenreResponse;
import com.cinema.service.AdminMovieService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController @RequestMapping("/api/v1/admin/genres")
public class AdminGenreController {
    private final AdminMovieService service;
    public AdminGenreController(AdminMovieService service) { this.service = service; }
    @GetMapping public ApiResponse<List<AdminGenreResponse>> list() { return ApiResponse.ok(service.genres()); }
}
