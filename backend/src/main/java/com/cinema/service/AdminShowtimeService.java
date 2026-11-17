package com.cinema.service;

import com.cinema.dto.catalog.AdminShowtimeResponse;
import com.cinema.dto.catalog.RoomResponse;
import com.cinema.dto.catalog.ShowtimeSlotRequest;
import com.cinema.entity.Movie;
import java.time.LocalDate;
import java.util.List;

public interface AdminShowtimeService {
    List<RoomResponse> rooms();
    List<AdminShowtimeResponse> showtimes(LocalDate date);
    List<AdminShowtimeResponse> createShowtimes(Long movieId, List<ShowtimeSlotRequest> slots);
    void createForMovie(Movie movie, List<ShowtimeSlotRequest> slots);
    void updateMovieDuration(Movie movie, int durationMinutes);
    AdminShowtimeResponse updateShowtime(Long showtimeId, ShowtimeSlotRequest request);
    void cancelShowtime(Long showtimeId);
}
