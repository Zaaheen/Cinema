package com.cinema.dto.price;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SeatPriceUpdateRequest(
        @NotNull Long rowVersion,
        @Size(max = 20) String seatType,
        @Size(max = 50) String movieVersion,
        BigDecimal amount,
        LocalDate validFrom,
        LocalDate validTo,
        @Size(max = 20) String status
) {}
