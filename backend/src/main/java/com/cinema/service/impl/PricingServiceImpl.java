package com.cinema.service.impl;

import com.cinema.dto.price.ResolvedSeatPrice;
import com.cinema.entity.Seat;
import com.cinema.entity.SeatPrice;
import com.cinema.entity.Showtime;
import com.cinema.exception.BusinessException;
import com.cinema.repository.SeatPriceRepository;
import com.cinema.service.PricingService;
import com.cinema.service.SeatPriceCanonicalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PricingServiceImpl implements PricingService {
    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final SeatPriceRepository seatPriceRepository;

    public PricingServiceImpl(SeatPriceRepository seatPriceRepository) {
        this.seatPriceRepository = seatPriceRepository;
    }

    @Override
    public ResolvedSeatPrice resolveSeatPrice(Showtime showtime, Seat seat) {
        if (showtime == null || seat == null || showtime.getRoom() == null || seat.getRoom() == null
                || !showtime.getRoom().getId().equals(seat.getRoom().getId())) {
            throw BusinessException.badRequest("PRICE_SEAT_ROOM_MISMATCH");
        }
        LocalDate showDate = showtime.getStartsAt().atZone(CINEMA_ZONE).toLocalDate();
        String versionKey = SeatPriceCanonicalizer.key(showtime.getMovie().getVersion());
        if (versionKey != null) {
            List<SeatPrice> exact = seatPriceRepository.findActiveExact(seat.getSeatType(), versionKey, showDate);
            ResolvedSeatPrice exactPrice = oneOrNull(exact, showDate);
            if (exactPrice != null) {
                return exactPrice;
            }
        }
        return generic(seat, showDate);
    }

    private ResolvedSeatPrice oneOrNull(List<SeatPrice> prices, LocalDate showDate) {
        if (prices.size() > 1) {
            throw BusinessException.conflict("PRICE_RULE_CONFLICT");
        }
        if (prices.size() == 1) {
            SeatPrice rule = prices.getFirst();
            return new ResolvedSeatPrice(rule.getPrice(), rule.getId(), rule.getRowVersion(), showDate);
        }
        return null;
    }

    private ResolvedSeatPrice generic(Seat seat, LocalDate showDate) {
        ResolvedSeatPrice fallback = oneOrNull(
                seatPriceRepository.findActiveGeneric(seat.getSeatType(), showDate), showDate);
        if (fallback == null) {
            throw BusinessException.conflict("PRICE_NOT_CONFIGURED");
        }
        return fallback;
    }
}
