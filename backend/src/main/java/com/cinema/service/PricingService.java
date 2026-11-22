package com.cinema.service;

import com.cinema.dto.price.ResolvedSeatPrice;
import com.cinema.entity.Seat;
import com.cinema.entity.Showtime;

public interface PricingService {
    ResolvedSeatPrice resolveSeatPrice(Showtime showtime, Seat seat);
}
