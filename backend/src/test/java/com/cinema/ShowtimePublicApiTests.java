package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Movie;
import com.cinema.entity.Room;
import com.cinema.entity.Showtime;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.ShowtimeRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShowtimePublicApiTests {

    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private EntityManager entityManager;
    @Autowired private MovieRepository movieRepository;
    @Autowired private ShowtimeRepository showtimeRepository;

    private MockMvc mockMvc;
    private int roomSequence;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void showtimesExposeOnlyScheduledSessionsStillOpenForOnlineSales() throws Exception {
        Movie closingSoonMovie = createMovie("NOW_SHOWING");
        Instant closingSoon = Instant.now().plus(Duration.ofMinutes(29));
        createShowtime(closingSoonMovie, closingSoon, "SCHEDULED");

        mockMvc.perform(get("/api/v1/movies/{movieId}/showtimes", closingSoonMovie.getId())
                        .param("date", localDate(closingSoon).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        Movie bookableMovie = createMovie("NOW_SHOWING");
        Instant bookable = Instant.now().plus(Duration.ofMinutes(31));
        Showtime bookableShowtime = createShowtime(bookableMovie, bookable, "SCHEDULED");

        mockMvc.perform(get("/api/v1/movies/{movieId}/showtimes", bookableMovie.getId())
                        .param("date", localDate(bookable).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(bookableShowtime.getId()))
                .andExpect(jsonPath("$.data[0].roomName").isNotEmpty());

        Movie statusMovie = createMovie("COMING_SOON");
        LocalDate tomorrow = LocalDate.now(CINEMA_ZONE).plusDays(1);
        Instant scheduledStart = tomorrow.atTime(10, 0).atZone(CINEMA_ZONE).toInstant();
        Showtime scheduled = createShowtime(statusMovie, scheduledStart, "SCHEDULED");
        createShowtime(statusMovie, scheduledStart.plus(Duration.ofHours(2)), "CANCELLED");

        mockMvc.perform(get("/api/v1/movies/{movieId}/showtimes", statusMovie.getId())
                        .param("date", tomorrow.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(scheduled.getId()));
    }

    @Test
    void showtimesReturnEmptyForPastDates() throws Exception {
        Movie movie = createMovie("COMING_SOON");

        mockMvc.perform(get("/api/v1/movies/{movieId}/showtimes", movie.getId())
                        .param("date", LocalDate.now(CINEMA_ZONE).minusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void showtimesHideNonPublicAndUnknownMovies() throws Exception {
        Movie stoppedMovie = createMovie("STOPPED");
        String today = LocalDate.now(CINEMA_ZONE).toString();

        mockMvc.perform(get("/api/v1/movies/{movieId}/showtimes", stoppedMovie.getId())
                        .param("date", today))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/movies/{movieId}/showtimes", 999999L)
                        .param("date", today))
                .andExpect(status().isNotFound());
    }

    private Movie createMovie(String status) {
        Movie movie = new Movie();
        movie.setTitle("Showtime test " + status + " " + System.nanoTime());
        movie.setDurationMinutes(120);
        movie.setVersion("2D");
        movie.setStatus(status);
        movie.setReleaseDate(LocalDate.now(CINEMA_ZONE));
        return movieRepository.saveAndFlush(movie);
    }

    private Showtime createShowtime(Movie movie, Instant startsAt, String status) {
        Room room = new Room();
        room.setName("Showtime room " + ++roomSequence + " " + System.nanoTime());
        room.setStatus("ACTIVE");
        entityManager.persist(room);
        entityManager.flush();

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartsAt(startsAt);
        showtime.setEndsAt(startsAt.plus(Duration.ofMinutes(120)));
        showtime.setStatus(status);
        return showtimeRepository.saveAndFlush(showtime);
    }

    private LocalDate localDate(Instant instant) {
        return instant.atZone(CINEMA_ZONE).toLocalDate();
    }
}
