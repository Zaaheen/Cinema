package com.cinema.repository;

import com.cinema.entity.Promotion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    @Query("select p from Promotion p where p.status = 'ACTIVE' and p.startsAt <= :now and p.endsAt >= :now order by p.endsAt asc")
    List<Promotion> findPublished(Instant now);

    @Query("select p from Promotion p where p.id = :id and p.status = 'ACTIVE' and p.startsAt <= :now and p.endsAt >= :now")
    Optional<Promotion> findPublishedById(Long id, Instant now);
}
