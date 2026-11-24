package com.cinema.repository;

import com.cinema.entity.SeatPriceAuditLog;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatPriceAuditLogRepository extends JpaRepository<SeatPriceAuditLog, Long> {
    List<SeatPriceAuditLog> findBySeatPriceIdInOrderByCreatedAtDescIdDesc(Collection<Long> seatPriceIds);
}
