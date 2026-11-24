package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.cinema.dto.price.ResolvedSeatPrice;
import com.cinema.entity.Movie;
import com.cinema.entity.Room;
import com.cinema.entity.Seat;
import com.cinema.entity.SeatPrice;
import com.cinema.entity.Showtime;
import com.cinema.exception.BusinessException;
import com.cinema.repository.SeatPriceRepository;
import com.cinema.service.impl.PricingServiceImpl;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PricingServiceTests {
    private final SeatPriceRepository repository = Mockito.mock(SeatPriceRepository.class);
    private final PricingServiceImpl pricingService = new PricingServiceImpl(repository);

    @Test
    void resolvesExactRuleForShowtimeDateBeforeGenericFallback() {
        Room room = room(1L);
        Seat seat = seat(room, "VIP");
        Showtime showtime = showtime(room, "IMAX   Laser", "2027-02-01T03:30:00Z");
        SeatPrice exact = rule(8L, new BigDecimal("180000"), 4L);
        when(repository.findActiveExact(eq("VIP"), eq("IMAX LASER"), eq(LocalDate.of(2027, 2, 1))))
                .thenReturn(List.of(exact));

        ResolvedSeatPrice resolved = pricingService.resolveSeatPrice(showtime, seat);

        assertEquals(new BigDecimal("180000"), resolved.amount());
        assertEquals(LocalDate.of(2027, 2, 1), resolved.showDate());
    }

    @Test
    void rejectsMissingOrAmbiguousPriceInsteadOfSelectingArbitrarily() {
        Room room = room(1L);
        Seat seat = seat(room, "STANDARD");
        Showtime showtime = showtime(room, "2D", "2027-02-01T03:30:00Z");
        when(repository.findActiveExact(eq("STANDARD"), eq("2D"), eq(LocalDate.of(2027, 2, 1))))
                .thenReturn(List.of());
        when(repository.findActiveGeneric(eq("STANDARD"), eq(LocalDate.of(2027, 2, 1))))
                .thenReturn(List.of());

        BusinessException missing = assertThrows(BusinessException.class,
                () -> pricingService.resolveSeatPrice(showtime, seat));
        assertEquals("PRICE_NOT_CONFIGURED", missing.getMessage());

        when(repository.findActiveGeneric(eq("STANDARD"), eq(LocalDate.of(2027, 2, 1))))
                .thenReturn(List.of(rule(1L, BigDecimal.ONE, 1L), rule(2L, BigDecimal.TEN, 1L)));
        BusinessException ambiguous = assertThrows(BusinessException.class,
                () -> pricingService.resolveSeatPrice(showtime, seat));
        assertEquals("PRICE_RULE_CONFLICT", ambiguous.getMessage());
    }

    private Room room(Long id) { Room room = new Room(); room.setId(id); return room; }
    private Seat seat(Room room, String type) { Seat seat = new Seat(); seat.setRoom(room); seat.setSeatType(type); return seat; }
    private Showtime showtime(Room room, String version, String startsAt) {
        Movie movie = new Movie(); movie.setVersion(version);
        Showtime showtime = new Showtime(); showtime.setRoom(room); showtime.setMovie(movie); showtime.setStartsAt(Instant.parse(startsAt)); return showtime;
    }
    private SeatPrice rule(Long id, BigDecimal amount, Long revision) {
        SeatPrice rule = new SeatPrice(); rule.setId(id); rule.setPrice(amount); rule.setRowVersion(revision); return rule;
    }
}
