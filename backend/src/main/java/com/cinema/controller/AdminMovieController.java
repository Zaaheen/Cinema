package com.cinema.controller;

import com.cinema.common.ApiResponse;
import com.cinema.dto.admin.movie.AdminMoviePatchRequest;
import com.cinema.dto.admin.movie.AdminMoviePosterResponse;
import com.cinema.dto.admin.movie.AdminMovieRequest;
import com.cinema.dto.admin.movie.AdminMovieResponse;
import com.cinema.dto.admin.movie.AdminMovieTrailerPatchRequest;
import com.cinema.service.AdminMovieService;
import com.cinema.common.PageResponse;
import com.cinema.dto.admin.movie.AdminGenreResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

@RestController
@Validated
@RequestMapping("/api/v1/admin/movies")
public class AdminMovieController {
    private final AdminMovieService adminMovieService;

    public AdminMovieController(AdminMovieService adminMovieService) {
        this.adminMovieService = adminMovieService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminMovieResponse>> list(@RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return ApiResponse.ok(adminMovieService.list(query, status, page, size));
    }

    @GetMapping("/{movieId}")
    public ApiResponse<AdminMovieResponse> get(@PathVariable Long movieId) { return ApiResponse.ok(adminMovieService.get(movieId)); }

    @PostMapping
    @Operation(summary = "Tạo phim quản trị không kèm suất chiếu")
    public ResponseEntity<ApiResponse<AdminMovieResponse>> create(@Valid @RequestBody AdminMovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo phim thành công", adminMovieService.create(request)));
    }

    @PatchMapping("/{movieId}")
    @Operation(summary = "Cập nhật thông tin phim quản trị")
    public ApiResponse<AdminMovieResponse> update(@PathVariable Long movieId, @RequestBody JsonNode requestBody) {
        return ApiResponse.ok("Cập nhật phim thành công",
                adminMovieService.update(movieId, AdminMoviePatchRequest.from(requestBody)));
    }

    @PostMapping(value = "/{movieId}/poster", consumes = "multipart/form-data")
    @Operation(summary = "Tải poster PNG lên Cloudinary và cập nhật phim")
    public ResponseEntity<ApiResponse<AdminMoviePosterResponse>> uploadPoster(
            @PathVariable Long movieId, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Cập nhật poster thành công",
                adminMovieService.uploadPoster(movieId, file)));
    }

    @PatchMapping(value = "/{movieId}/trailer", consumes = "application/json")
    @Operation(summary = "Cập nhật hoặc xóa trailer YouTube của phim")
    public ApiResponse<AdminMovieResponse> updateTrailer(@PathVariable Long movieId, @RequestBody JsonNode requestBody) {
        return ApiResponse.ok("Cập nhật trailer thành công",
                adminMovieService.updateTrailer(movieId, AdminMovieTrailerPatchRequest.from(requestBody)));
    }
}
