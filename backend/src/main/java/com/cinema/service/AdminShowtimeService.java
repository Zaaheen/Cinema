package com.cinema.service;

import com.cinema.dto.admin.showtime.AdminShowtimeCreateRequest;
import com.cinema.dto.admin.showtime.AdminShowtimePatchRequest;
import com.cinema.dto.admin.showtime.AdminShowtimeResponse;
import java.time.LocalDate;
import java.util.List;

public interface AdminShowtimeService {
    AdminShowtimeResponse create(AdminShowtimeCreateRequest request);
    List<AdminShowtimeResponse> list(LocalDate date);
    AdminShowtimeResponse get(Long showtimeId);
    AdminShowtimeResponse update(Long showtimeId, AdminShowtimePatchRequest request);
    AdminShowtimeResponse cancel(Long showtimeId);
}
