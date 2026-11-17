package com.cinema.dto.catalog;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;

public record ShowtimeSlotRequest(
        @NotNull @Positive Long roomId,
        @NotNull OffsetDateTime startsAt
) {}
