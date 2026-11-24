package com.cinema.repository;

import com.cinema.entity.Movie;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "genres")
    Page<Movie> findAll(org.springframework.data.jpa.domain.Specification<Movie> specification, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "genres")
    Optional<Movie> findWithGenresById(Long id);
    Optional<Movie> findByIdAndStatusIn(Long id, Collection<String> statuses);
    List<Movie> findByStatusOrderByReleaseDateDescIdDesc(String status);
    List<Movie> findByStatusOrderByReleaseDateAscIdAsc(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select movie from Movie movie where movie.id = :movieId")
    Optional<Movie> findByIdForUpdate(@Param("movieId") Long movieId);
}
