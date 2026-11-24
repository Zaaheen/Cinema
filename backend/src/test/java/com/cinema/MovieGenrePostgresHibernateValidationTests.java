package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cinema.dto.catalog.MovieDetailResponse;
import com.cinema.dto.catalog.MovieSummaryResponse;
import com.cinema.entity.Genre;
import com.cinema.entity.Movie;
import com.cinema.repository.GenreRepository;
import com.cinema.repository.MovieRepository;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.profiles.active=local",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers(disabledWithoutDocker = true)
class MovieGenrePostgresHibernateValidationTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_movie_genre_hibernate_test")
            .withUsername("cinema")
            .withPassword("cinema");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-key-for-movie-genre-validation-0123456789abcdef");
        registry.add("app.jwt.refresh-cookie-secure", () -> false);
    }

    @Autowired private MovieRepository movieRepository;
    @Autowired private GenreRepository genreRepository;

    @Test
    void validatesMappingsWithoutChangingLegacyCatalogDtosOrStitchMetadata() {
        Movie movie = new Movie();
        movie.setTitle("Genre validation");
        movie.setGenre("Hành động");
        movie.setDurationMinutes(100);
        movie.setVersion("2D");
        movie.setStatus("NOW_SHOWING");
        movie.setBackdropUrl("https://example.test/backdrop.jpg");
        movie.setAgeRating("T13");
        movie.setSourceKey("genre-validation");
        movie.setCreatedAt(Instant.now());
        movie.setUpdatedAt(Instant.now());
        Genre genre = genreRepository.findByName("Hành động").orElseThrow();
        movie.setGenres(Set.of(genre));
        Movie persisted = movieRepository.saveAndFlush(movie);

        assertEquals("Hành động", persisted.getGenre());
        assertEquals("https://example.test/backdrop.jpg", persisted.getBackdropUrl());
        assertEquals("T13", persisted.getAgeRating());
        assertEquals("genre-validation", persisted.getSourceKey());
        assertEquals(1, persisted.getGenres().size());
        assertEquals("Hành động", MovieSummaryResponse.from(persisted).genre());
        assertEquals("https://example.test/backdrop.jpg", MovieDetailResponse.from(persisted).backdropUrl());
        assertEquals("T13", MovieDetailResponse.from(persisted).ageRating());
    }
}
