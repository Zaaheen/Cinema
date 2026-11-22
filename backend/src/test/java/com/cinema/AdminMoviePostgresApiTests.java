package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Genre;
import com.cinema.entity.Role;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.GenreRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import java.util.UUID;
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
class AdminMoviePostgresApiTests {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_admin_movie_test").withUsername("cinema").withPassword("cinema");

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-key-for-admin-movie-postgres-tests-0123456789abcdef");
        registry.add("app.jwt.refresh-cookie-secure", () -> false);
    }

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy filter;
    @Autowired private GenreRepository genres;
    @Autowired private MovieRepository movies;
    @Autowired private RoleRepository roles;
    @Autowired private UserRepository users;
    @Autowired private JwtService jwt;

    @Test
    void usesPostgresSixteenAndV7CanonicalGenresForAdminCreate() throws Exception {
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build();
        Genre action = genres.findByName("Hành động").orElseThrow();
        Genre adventure = genres.findByName("Phiêu lưu").orElseThrow();
        String body = "{\"title\":\"Postgres movie\",\"primaryGenreId\":%d,\"genreIds\":[%d,%d],"
                + "\"durationMinutes\":100,\"version\":\"2D\",\"posterUrl\":\"https://example.test/poster.jpg\"}";
        body = body.formatted(action.getId(), action.getId(), adventure.getId());
        mvc.perform(post("/api/v1/admin/movies").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("COMING_SOON"))
                .andExpect(jsonPath("$.data.genres.length()").value(2));
        assertEquals("Hành động", movies.findAll().getFirst().getGenre());
        mvc.perform(post("/api/v1/admin/movies").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    private String bearer() {
        Role role = roles.findByCode("ADMIN").orElseThrow();
        User user = new User(); user.setUsername("pg-admin-" + UUID.randomUUID()); user.setPasswordHash("hash");
        user.setFullName("Postgres Admin"); user.setStatus(UserStatus.ACTIVE); user.setRole(role);
        return "Bearer " + jwt.generateAccessToken(users.saveAndFlush(user));
    }
}
