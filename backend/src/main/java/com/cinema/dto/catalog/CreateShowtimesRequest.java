package com.cinema.dto.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateShowtimesRequest(
        @NotNull @Positive Long movieId,
        @NotNull @Size(min = 1, max = 100, message = "Chọn từ 1 đến 100 suất chiếu")
        List<@NotNull @Valid ShowtimeSlotRequest> showtimes
) {}
