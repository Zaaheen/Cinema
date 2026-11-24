package com.cinema.repository;

import com.cinema.entity.Genre;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    List<Genre> findBySelectableTrueOrderByNameAscIdAsc();
    Optional<Genre> findByName(String name);
}
