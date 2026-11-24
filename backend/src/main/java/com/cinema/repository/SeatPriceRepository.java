package com.cinema.repository;

import com.cinema.entity.SeatPrice;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SeatPriceRepository extends JpaRepository<SeatPrice, Long> {
    @Query("select p from SeatPrice p where p.validFrom <= :today and (p.validTo is null or p.validTo >= :today) order by p.seatType, p.movieVersion")
    List<SeatPrice> findEffectiveOn(LocalDate today);

    @Query("""
            select p from SeatPrice p
            where p.origin = 'ADMIN_APPROVED'
              and (:status is null or p.status = :status)
              and (:seatType is null or p.seatType = :seatType)
              and (:movieVersionKey is null or p.movieVersionKey = :movieVersionKey)
              and (:effectiveOn is null or (p.validFrom <= :effectiveOn and (p.validTo is null or p.validTo >= :effectiveOn)))
            """)
    Page<SeatPrice> findAdminRules(String status, String seatType, String movieVersionKey,
            LocalDate effectiveOn, Pageable pageable);

    @Query("""
            select p from SeatPrice p
            where p.origin = 'ADMIN_APPROVED' and p.status = 'ACTIVE'
              and p.seatType = :seatType and p.movieVersionKey = :movieVersionKey
              and p.validFrom <= :effectiveOn and (p.validTo is null or p.validTo >= :effectiveOn)
            """)
    List<SeatPrice> findActiveExact(String seatType, String movieVersionKey, LocalDate effectiveOn);

    @Query("""
            select p from SeatPrice p
            where p.origin = 'ADMIN_APPROVED' and p.status = 'ACTIVE'
              and p.seatType = :seatType and p.movieVersionKey is null
              and p.validFrom <= :effectiveOn and (p.validTo is null or p.validTo >= :effectiveOn)
            """)
    List<SeatPrice> findActiveGeneric(String seatType, LocalDate effectiveOn);

    @Query("""
            select p from SeatPrice p
            where p.origin = 'ADMIN_APPROVED' and p.status = 'ACTIVE'
              and p.validFrom <= :effectiveOn and (p.validTo is null or p.validTo >= :effectiveOn)
            order by p.seatType asc, p.movieVersionKey asc nulls first, p.validFrom asc, p.id asc
            """)
    List<SeatPrice> findPublicEffectiveOn(LocalDate effectiveOn);
}
