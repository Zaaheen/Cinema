package com.cinema.dto.price;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SeatPriceRequest(
        @NotBlank @Size(max = 20) String seatType,
        @Size(max = 50) String movieVersion,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull LocalDate validFrom,
        LocalDate validTo
) {}
