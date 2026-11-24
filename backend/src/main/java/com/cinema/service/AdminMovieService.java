package com.cinema.service;

import com.cinema.dto.admin.movie.AdminMoviePatchRequest;
import com.cinema.dto.admin.movie.AdminMovieBannerResponse;
import com.cinema.dto.admin.movie.AdminMoviePosterResponse;
import com.cinema.dto.admin.movie.AdminMovieRequest;
import com.cinema.dto.admin.movie.AdminMovieResponse;
import com.cinema.dto.admin.movie.AdminMovieTrailerPatchRequest;
import org.springframework.web.multipart.MultipartFile;
import com.cinema.common.PageResponse;
import com.cinema.dto.admin.movie.AdminGenreResponse;
import java.util.List;

public interface AdminMovieService {
    PageResponse<AdminMovieResponse> list(String query, String status, int page, int size);
    AdminMovieResponse get(Long movieId);
    List<AdminGenreResponse> genres();
    AdminMovieResponse create(AdminMovieRequest request);
    AdminMovieResponse update(Long movieId, AdminMoviePatchRequest request);
    AdminMoviePosterResponse uploadPoster(Long movieId, MultipartFile file);
    AdminMovieBannerResponse uploadBanner(Long movieId, MultipartFile file);
    AdminMovieResponse updateTrailer(Long movieId, AdminMovieTrailerPatchRequest request);
}
