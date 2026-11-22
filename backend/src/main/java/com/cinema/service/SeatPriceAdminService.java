package com.cinema.service;

import com.cinema.common.PageResponse;
import com.cinema.dto.price.SeatPriceAdminResponse;
import com.cinema.dto.price.SeatPriceRequest;
import com.cinema.dto.price.SeatPriceUpdateRequest;
import java.time.LocalDate;

public interface SeatPriceAdminService {
    PageResponse<SeatPriceAdminResponse> list(String status, String seatType, String movieVersion,
            LocalDate effectiveOn, int page, int size);
    SeatPriceAdminResponse createDraft(SeatPriceRequest request);
    SeatPriceAdminResponse update(Long id, SeatPriceUpdateRequest request);
}
