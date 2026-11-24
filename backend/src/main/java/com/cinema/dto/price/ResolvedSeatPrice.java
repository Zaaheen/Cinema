package com.cinema.dto.price;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResolvedSeatPrice(BigDecimal amount, Long priceRuleId, Long priceRuleRevision,
        LocalDate showDate) {}
