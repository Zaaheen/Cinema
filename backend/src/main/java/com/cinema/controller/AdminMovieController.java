package com.cinema.controller;

import com.cinema.common.ApiResponse;
import com.cinema.dto.catalog.CreateMovieRequest;
import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.PosterUploadResponse;
import com.cinema.dto.catalog.UpdateMovieRequest;
import com.cinema.service.AdminMovieService;
import com.cinema.service.CloudinaryPosterService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/movies")
public class AdminMovieController {

    private final AdminMovieService adminMovieService;
    private final CloudinaryPosterService cloudinaryPosterService;

    public AdminMovieController(
            AdminMovieService adminMovieService,
            CloudinaryPosterService cloudinaryPosterService) {
        this.adminMovieService = adminMovieService;
        this.cloudinaryPosterService = cloudinaryPosterService;
    }

    @PostMapping
    @Operation(summary = "Tạo phim từ dữ liệu JSON và poster URL tùy chọn")
    public ResponseEntity<ApiResponse<MovieDetailResponse>> createMovie(
            @Valid @RequestBody CreateMovieRequest request) {
        MovieDetailResponse movie = adminMovieService.createMovie(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo phim thành công", movie));
    }

    @PatchMapping("/{movieId}")
    @Operation(summary = "Chỉnh sửa thông tin chi tiết phim")
    public ResponseEntity<ApiResponse<MovieDetailResponse>> updateMovie(
            @PathVariable Long movieId,
            @Valid @RequestBody UpdateMovieRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật phim thành công", adminMovieService.updateMovie(movieId, request)));
    }

    @PostMapping(value = "/poster", consumes = "multipart/form-data")
    @Operation(summary = "Tải ảnh poster PNG lên Cloudinary")
    public ResponseEntity<ApiResponse<PosterUploadResponse>> uploadPoster(
            @RequestPart("file") MultipartFile file) {
        String posterUrl = cloudinaryPosterService.uploadPng(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Tải poster lên Cloudinary thành công", new PosterUploadResponse(posterUrl)));
    }
}
