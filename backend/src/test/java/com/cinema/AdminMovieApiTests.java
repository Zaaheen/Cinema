package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Genre;
import com.cinema.entity.Movie;
import com.cinema.entity.Role;
import com.cinema.entity.Room;
import com.cinema.entity.Showtime;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.GenreRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.RoomRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AdminMovieApiTests {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private MovieRepository movieRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    private MockMvc mockMvc;
    private Map<String, Long> canonicalGenreIds;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        showtimeRepository.deleteAll();
        movieRepository.deleteAll();
        roomRepository.deleteAll();
        genreRepository.deleteAll();
        canonicalGenreIds = genreRepository.saveAll(List.of(
                genre("Hành động", true), genre("Hài kịch", true), genre("Kinh dị", true),
                genre("Khoa học viễn tưởng", true), genre("Tình cảm", true), genre("Phiêu lưu", true)))
                .stream().collect(Collectors.toMap(Genre::getName, Genre::getId));
    }

    @Test
    void adminCreatesMovieWithCanonicalGenreMappingsAndSafeDefaults() throws Exception {
        mockMvc.perform(post("/api/v1/admin/movies").header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(createRequest(
                                List.of(canonicalGenreIds.get("Hành động"), canonicalGenreIds.get("Phiêu lưu")),
                                canonicalGenreIds.get("Hành động"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("COMING_SOON"))
                .andExpect(jsonPath("$.data.genre").value("Hành động"))
                .andExpect(jsonPath("$.data.primaryGenreId").value(canonicalGenreIds.get("Hành động")))
                .andExpect(jsonPath("$.data.genres.length()").value(2))
                .andExpect(jsonPath("$.data.trailerUrl").doesNotExist())
                .andExpect(jsonPath("$.data.sourceKey").doesNotExist());

        Movie created = movieRepository.findAll().getFirst();
        assertEquals("Hành động", created.getGenre());
        assertEquals("COMING_SOON", created.getStatus());
        assertNull(created.getTrailerUrl());
        assertNull(created.getSourceKey());
    }

    @Test
    void validatesGenreSelectionAndHttpsImageUrls() throws Exception {
        String admin = bearer("ADMIN");
        mockMvc.perform(post("/api/v1/admin/movies").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(createRequest(
                                List.of(canonicalGenreIds.get("Hành động"), canonicalGenreIds.get("Hành động")),
                                canonicalGenreIds.get("Hành động"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/admin/movies").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(createRequest(
                                List.of(999999L), 999999L)))
                .andExpect(status().isBadRequest());
        Genre legacy = genreRepository.saveAndFlush(genre("Sci-Fi cổ", false));
        mockMvc.perform(post("/api/v1/admin/movies").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(createRequest(List.of(legacy.getId()), legacy.getId())))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/admin/movies").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(List.of(canonicalGenreIds.get("Hành động")), canonicalGenreIds.get("Phiêu lưu"))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/admin/movies").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createRequest(List.of(canonicalGenreIds.get("Hành động")), canonicalGenreIds.get("Hành động"))
                                .replace("https://example.test/poster.jpg", "http://example.test/poster.jpg")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminPatchPreservesStatusAndLegacyGenresUnlessGenreChangeIsExplicit() throws Exception {
        Genre legacy = genreRepository.saveAndFlush(genre("Science fiction", false));
        Movie movie = legacyMovie(legacy);
        Long action = canonicalGenreIds.get("Hành động");
        mockMvc.perform(patch("/api/v1/admin/movies/{id}", movie.getId()).header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Tên mới\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("STOPPED"))
                .andExpect(jsonPath("$.data.genre").value("Science fiction"));
        Movie unchangedGenres = movieRepository.findById(movie.getId()).orElseThrow();
        assertEquals("Science fiction", unchangedGenres.getGenre());

        mockMvc.perform(patch("/api/v1/admin/movies/{id}", movie.getId()).header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"genreIds\":[" + action + "],\"primaryGenreId\":" + action + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.genre").value("Hành động"))
                .andExpect(jsonPath("$.data.genres.length()").value(2));
        Movie changed = movieRepository.findById(movie.getId()).orElseThrow();
        assertEquals("Hành động", changed.getGenre());
    }

    @Test
    void patchRejectsExplicitNullUnsupportedFieldsAndProtectedFieldChanges() throws Exception {
        Movie movie = canonicalMovie();
        String admin = bearer("ADMIN");
        for (String body : List.of("{\"title\":null}", "{\"status\":\"STOPPED\"}",
                "{\"sourceKey\":\"x\"}", "{\"trailerUrl\":\"https://example.test/trailer\"}")) {
            mockMvc.perform(patch("/api/v1/admin/movies/{id}", movie.getId()).header("Authorization", admin)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(patch("/api/v1/admin/movies/{id}", movie.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"genreIds\":[1]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void durationChangeIsAllowedWithoutShowtimesAndRejectedWhenNonCancelledShowtimeExists() throws Exception {
        Movie movie = canonicalMovie();
        String admin = bearer("ADMIN");
        mockMvc.perform(patch("/api/v1/admin/movies/{id}", movie.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"durationMinutes\":101}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.durationMinutes").value(101));
        Room room = new Room(); room.setName("Slice 2 Room"); room.setStatus("ACTIVE"); room = roomRepository.saveAndFlush(room);
        Showtime showtime = new Showtime(); showtime.setMovie(movieRepository.findById(movie.getId()).orElseThrow());
        showtime.setRoom(room); showtime.setStartsAt(Instant.now().plusSeconds(7200));
        showtime.setEndsAt(Instant.now().plusSeconds(10800)); showtime.setStatus("SCHEDULED");
        showtimeRepository.saveAndFlush(showtime);
        mockMvc.perform(patch("/api/v1/admin/movies/{id}", movie.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"durationMinutes\":102}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enforcesAdminAuthorizationAndNotFound() throws Exception {
        String body = createRequest(List.of(canonicalGenreIds.get("Hành động")), canonicalGenreIds.get("Hành động"));
        mockMvc.perform(post("/api/v1/admin/movies").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        for (String role : List.of("MEMBER", "EMPLOYEE")) {
            mockMvc.perform(post("/api/v1/admin/movies").header("Authorization", bearer(role))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(patch("/api/v1/admin/movies/{id}", 999999L).header("Authorization", bearer("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Không tồn tại\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminReadApisExposeAllStatusesMetadataGenresAndCanonicalGenres() throws Exception {
        Movie stopped = canonicalMovie();
        stopped.setStatus("STOPPED"); stopped.setPosterUrl("https://example.test/poster.png");
        stopped.setTrailerUrl("https://www.youtube.com/watch?v=abcdefghijk"); stopped = movieRepository.saveAndFlush(stopped);
        String admin = bearer("ADMIN");
        mockMvc.perform(get("/api/v1/admin/movies").header("Authorization", admin).param("q", "  metadata  ").param("status", "STOPPED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(stopped.getId()))
                .andExpect(jsonPath("$.data.content[0].status").value("STOPPED"));
        mockMvc.perform(get("/api/v1/admin/movies/{id}", stopped.getId()).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.primaryGenreId").value(canonicalGenreIds.get("Hành động")))
                .andExpect(jsonPath("$.data.posterUrl").value("https://example.test/poster.png"))
                .andExpect(jsonPath("$.data.trailerUrl").value("https://www.youtube.com/watch?v=abcdefghijk"));
        mockMvc.perform(get("/api/v1/admin/genres").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(6));
        mockMvc.perform(get("/api/v1/admin/movies").header("Authorization", admin).param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/admin/movies")).andExpect(status().isUnauthorized());
        for (String role : List.of("MEMBER", "EMPLOYEE")) {
            mockMvc.perform(get("/api/v1/admin/genres").header("Authorization", bearer(role))).andExpect(status().isForbidden());
        }
    }

    private Movie canonicalMovie() {
        Movie movie = new Movie(); movie.setTitle("Metadata movie"); movie.setGenre("Hành động");
        movie.setGenres(new LinkedHashSet<>(List.of(genreRepository.findById(canonicalGenreIds.get("Hành động")).orElseThrow())));
        movie.setDurationMinutes(100); movie.setVersion("2D"); movie.setStatus("STOPPED");
        movie.setBackdropUrl("https://example.test/old-backdrop.jpg"); movie.setAgeRating("T13");
        movie.setSourceKey("stitch-source"); movie.setTrailerUrl("https://example.test/existing-trailer");
        return movieRepository.saveAndFlush(movie);
    }

    private Movie legacyMovie(Genre legacy) {
        Movie movie = new Movie(); movie.setTitle("Legacy movie"); movie.setGenre("Science fiction");
        movie.setGenres(new LinkedHashSet<>(List.of(legacy))); movie.setDurationMinutes(100); movie.setVersion("2D");
        movie.setStatus("STOPPED"); return movieRepository.saveAndFlush(movie);
    }

    private String createRequest(List<Long> genres, Long primary) {
        return """
                {"title":"  Phim quản trị  ","primaryGenreId":%d,"genreIds":%s,
                 "releaseDate":"2027-01-01","durationMinutes":120,"version":" 2D ",
                 "posterUrl":"https://example.test/poster.jpg","backdropUrl":"https://example.test/backdrop.jpg","ageRating":"T13"}
                """.formatted(primary, genres);
    }

    private Genre genre(String name, boolean selectable) {
        Genre genre = new Genre(); genre.setName(name); genre.setSelectable(selectable); return genre;
    }

    private String bearer(String roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseGet(() -> {
            Role created = new Role(); created.setCode(roleCode); created.setName(roleCode); return roleRepository.save(created);
        });
        User user = new User(); user.setUsername("slice2-" + UUID.randomUUID()); user.setPasswordHash("hash");
        user.setFullName("Slice 2"); user.setStatus(UserStatus.ACTIVE); user.setRole(role);
        return "Bearer " + jwtService.generateAccessToken(userRepository.saveAndFlush(user));
    }
}
