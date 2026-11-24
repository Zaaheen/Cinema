package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cinema.entity.Movie;
import com.cinema.entity.Room;
import com.cinema.entity.Showtime;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.RoomRepository;
import com.cinema.repository.ShowtimeRepository;
import java.time.Instant;
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
class ShowtimeOverlapPostgresHibernateValidationTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_showtime_overlap_hibernate_test")
            .withUsername("cinema")
            .withPassword("cinema");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-key-for-showtime-overlap-validation-0123456789abcdef");
        registry.add("app.jwt.refresh-cookie-secure", () -> false);
    }

    @Autowired private MovieRepository movieRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private ShowtimeRepository showtimeRepository;

    @Test
    void validatesExistingShowtimeMappingWithoutAdditionalColumns() {
        Movie movie = new Movie();
        movie.setTitle("V8 Hibernate validation");
        movie.setDurationMinutes(120);
        movie.setVersion("2D");
        movie.setStatus("NOW_SHOWING");
        movie = movieRepository.saveAndFlush(movie);
        Room room = new Room();
        room.setName("V8 Hibernate room");
        room.setStatus("ACTIVE");
        room = roomRepository.saveAndFlush(room);
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartsAt(Instant.parse("2027-03-08T03:00:00Z"));
        showtime.setEndsAt(Instant.parse("2027-03-08T05:00:00Z"));
        showtime.setStatus("SCHEDULED");

        Showtime saved = showtimeRepository.saveAndFlush(showtime);
        assertEquals("SCHEDULED", saved.getStatus());
        assertEquals(room.getId(), saved.getRoom().getId());
    }
}
