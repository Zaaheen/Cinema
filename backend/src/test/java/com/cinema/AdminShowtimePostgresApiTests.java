package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Movie;
import com.cinema.entity.Role;
import com.cinema.entity.Room;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.RoomRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.profiles.active=local")
@Testcontainers(disabledWithoutDocker = true)
class AdminShowtimePostgresApiTests {
    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_admin_showtime_test").withUsername("cinema").withPassword("cinema");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-key-for-admin-showtime-postgres-tests-0123456789abcdef");
        registry.add("app.jwt.refresh-cookie-secure", () -> false);
    }
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy filter;
    @Autowired private MovieRepository movies;
    @Autowired private RoomRepository rooms;
    @Autowired private ShowtimeRepository showtimes;
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private JwtService jwt;

    @Test
    void mapsPostgresV8ExclusionConstraintToApprovedConflictCode() throws Exception {
        Movie movie = new Movie(); movie.setTitle("PG admin showtime " + UUID.randomUUID()); movie.setDurationMinutes(120);
        movie.setVersion("2D"); movie.setStatus("NOW_SHOWING"); movie = movies.saveAndFlush(movie);
        Room room = new Room(); room.setName("PG admin room " + UUID.randomUUID()); room.setStatus("ACTIVE"); room = rooms.saveAndFlush(room);
        String start = LocalDate.now(CINEMA_ZONE).plusDays(2) + "T10:00:00+07:00";
        String overlapping = LocalDate.now(CINEMA_ZONE).plusDays(2) + "T10:30:00+07:00";
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build();
        String authorization = "Authorization";
        mvc.perform(post("/api/v1/admin/showtimes").header(authorization, bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(request(movie.getId(), room.getId(), start))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/admin/showtimes").header(authorization, bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(request(movie.getId(), room.getId(), overlapping)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("SHOWTIME_ROOM_CONFLICT"));
    }

    @Test
    void concurrentCreatesCommitExactlyOneAndMapTheOtherToV8Conflict() throws Exception {
        Movie movie = movie(120); Room room = room("concurrent create");
        String day = LocalDate.now(CINEMA_ZONE).plusDays(3).toString();
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build();
        CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> concurrentCreate(mvc, ready, start, movie.getId(), room.getId(), day + "T10:00:00+07:00"));
            Future<Integer> second = executor.submit(() -> concurrentCreate(mvc, ready, start, movie.getId(), room.getId(), day + "T10:30:00+07:00"));
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            java.util.List<Integer> statuses = java.util.List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
            assertEquals(1, statuses.stream().filter(value -> value == 201).count());
            assertEquals(1, statuses.stream().filter(value -> value == 409).count());
            assertEquals(1, movieshowtimeCount(movie.getId(), room.getId()));
        } finally { start.countDown(); executor.shutdownNow(); executor.awaitTermination(10, TimeUnit.SECONDS); }
    }

    @Test
    void concurrentPatchesCommitExactlyOneAndPreserveRejectedShowtime() throws Exception {
        Movie movie = movie(90); Room firstRoom = room("first"); Room secondRoom = room("second"); Room targetRoom = room("target");
        String day = LocalDate.now(CINEMA_ZONE).plusDays(4).toString();
        Long first = create(movie, firstRoom, day + "T10:00:00+07:00");
        Long second = create(movie, secondRoom, day + "T10:00:00+07:00");
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build();
        CountDownLatch ready = new CountDownLatch(2); CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> one = executor.submit(() -> concurrentPatch(mvc, ready, start, first, targetRoom.getId(), day + "T10:00:00+07:00"));
            Future<Integer> two = executor.submit(() -> concurrentPatch(mvc, ready, start, second, targetRoom.getId(), day + "T10:00:00+07:00"));
            org.junit.jupiter.api.Assertions.assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            java.util.List<Integer> statuses = java.util.List.of(one.get(30, TimeUnit.SECONDS), two.get(30, TimeUnit.SECONDS));
            assertEquals(1, statuses.stream().filter(value -> value == 200).count()); assertEquals(1, statuses.stream().filter(value -> value == 409).count());
            assertEquals(1, movieshowtimeCount(movie.getId(), targetRoom.getId()));
        } finally { start.countDown(); executor.shutdownNow(); executor.awaitTermination(10, TimeUnit.SECONDS); }
    }

    private int concurrentCreate(MockMvc mvc, CountDownLatch ready, CountDownLatch start, Long movieId, Long roomId, String startsAt) throws Exception {
        ready.countDown(); org.junit.jupiter.api.Assertions.assertTrue(start.await(10, TimeUnit.SECONDS));
        return mvc.perform(post("/api/v1/admin/showtimes").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON).content(request(movieId, roomId, startsAt))).andReturn().getResponse().getStatus();
    }
    private int concurrentPatch(MockMvc mvc, CountDownLatch ready, CountDownLatch start, Long showtimeId, Long roomId, String startsAt) throws Exception {
        ready.countDown(); org.junit.jupiter.api.Assertions.assertTrue(start.await(10, TimeUnit.SECONDS));
        return mvc.perform(patch("/api/v1/admin/showtimes/{id}", showtimeId).header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON).content("{\"roomId\":" + roomId + ",\"startsAt\":\"" + startsAt + "\"}")).andReturn().getResponse().getStatus();
    }
    private Movie movie(int duration) { Movie value = new Movie(); value.setTitle("PG movie " + UUID.randomUUID()); value.setDurationMinutes(duration); value.setVersion("2D"); value.setStatus("NOW_SHOWING"); return movies.saveAndFlush(value); }
    private Room room(String suffix) { Room value = new Room(); value.setName("PG-" + suffix + "-" + UUID.randomUUID().toString().substring(0, 8)); value.setStatus("ACTIVE"); return rooms.saveAndFlush(value); }
    private Long create(Movie movie, Room room, String startsAt) throws Exception { MockMvc mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build(); String response=mvc.perform(post("/api/v1/admin/showtimes").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON).content(request(movie.getId(), room.getId(), startsAt))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(); return Long.parseLong(response.replaceFirst("(?s).*\\\"id\\\":(\\d+).*", "$1")); }
    private long movieshowtimeCount(Long movieId, Long roomId) { return showtimes.findAll().stream().filter(value -> value.getMovie().getId().equals(movieId) && value.getRoom().getId().equals(roomId)).count(); }

    private String request(Long movieId, Long roomId, String startsAt) {
        return "{\"movieId\":" + movieId + ",\"roomId\":" + roomId + ",\"startsAt\":\"" + startsAt + "\"}";
    }
    private String bearer() {
        Role role = roles.findByCode("ADMIN").orElseThrow();
        User user = new User(); user.setUsername("pg-st-" + UUID.randomUUID()); user.setPasswordHash("hash");
        user.setFullName("PG Showtime Admin"); user.setStatus(UserStatus.ACTIVE); user.setRole(role);
        return "Bearer " + jwt.generateAccessToken(users.saveAndFlush(user));
    }
}
