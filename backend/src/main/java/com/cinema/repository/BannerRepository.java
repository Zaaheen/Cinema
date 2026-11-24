package com.cinema.repository;

import com.cinema.entity.Banner;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByActiveTrueOrderByDisplayOrderAscIdAsc();
}
