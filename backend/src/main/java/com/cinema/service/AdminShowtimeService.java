package com.cinema.service;

import com.cinema.dto.admin.showtime.AdminShowtimeCreateRequest;
import com.cinema.dto.admin.showtime.AdminShowtimePatchRequest;
import com.cinema.dto.admin.showtime.AdminShowtimeResponse;
import com.cinema.dto.admin.showtime.AdminRoomResponse;
import java.time.LocalDate;
import java.util.List;

public interface AdminShowtimeService {
    AdminShowtimeResponse create(AdminShowtimeCreateRequest request);
    List<AdminShowtimeResponse> list(LocalDate date);
    List<AdminRoomResponse> listRooms();
    AdminShowtimeResponse get(Long showtimeId);
    AdminShowtimeResponse update(Long showtimeId, AdminShowtimePatchRequest request);
    AdminShowtimeResponse cancel(Long showtimeId);
}
