package com.cinema.repository;

import com.cinema.entity.SeatPrice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SeatPriceRepository extends JpaRepository<SeatPrice, Long> {
    @Query("select p from SeatPrice p where p.validFrom <= :today and (p.validTo is null or p.validTo >= :today) order by p.seatType, p.movieVersion")
    List<SeatPrice> findEffectiveOn(LocalDate today);
}
