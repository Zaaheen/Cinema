package com.cinema.repository;

import com.cinema.entity.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByRoomIdOrderByRowLabelAscSeatNumberAsc(Long roomId);
}
