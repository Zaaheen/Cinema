package com.cinema.service;

import com.cinema.dto.catalog.CreateMovieRequest;
import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.UpdateMovieRequest;

public interface AdminMovieService {
    MovieDetailResponse createMovie(CreateMovieRequest request);
    MovieDetailResponse updateMovie(Long movieId, UpdateMovieRequest request);
}
