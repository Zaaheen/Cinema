package com.cinema.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String title;
    private String genre;
    @Column(name = "release_date") private LocalDate releaseDate;
    @Column(name = "duration_minutes", nullable = false) private int durationMinutes;
    @Column(nullable = false) private String version;
    @Column(name = "cast_text") private String castText;
    private String director;
    private String studio;
    private String synopsis;
    @Column(name = "trailer_url") private String trailerUrl;
    @Column(name = "poster_url") private String posterUrl;
    @Column(nullable = false) private String status;
    @Column(name = "created_at", updatable = false) private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
