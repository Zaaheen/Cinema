package com.cinema.repository;

import com.cinema.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long>,
        JpaSpecificationExecutor<Movie> {

    Optional<Movie> findByIdAndStatusIn(Long id, Collection<String> statuses);
}