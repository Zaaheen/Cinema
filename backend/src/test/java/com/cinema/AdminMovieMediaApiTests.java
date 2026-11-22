package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Movie;
import com.cinema.entity.Role;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import com.cinema.service.CloudinaryPosterService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Import(AdminMovieMediaApiTests.MockPosterStorage.class)
class AdminMovieMediaApiTests {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private MovieRepository movieRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private CloudinaryPosterService cloudinaryPosterService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        movieRepository.deleteAll();
        reset(cloudinaryPosterService);
    }

    @Test
    void adminUploadsPosterAndOnlyReceivesSafeMovieResponse() throws Exception {
        Movie movie = movie();
        when(cloudinaryPosterService.uploadPng(any())).thenReturn("https://res.cloudinary.com/demo/image/upload/poster.png");
        MockMultipartFile file = new MockMultipartFile("file", "poster.png", "image/png", new byte[] {1});

        mockMvc.perform(multipart("/api/v1/admin/movies/{id}/poster", movie.getId()).file(file)
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.movieId").value(movie.getId()))
                .andExpect(jsonPath("$.data.posterUrl").value("https://res.cloudinary.com/demo/image/upload/poster.png"))
                .andExpect(jsonPath("$.data.publicId").doesNotExist());
        assertEquals("https://res.cloudinary.com/demo/image/upload/poster.png",
                movieRepository.findById(movie.getId()).orElseThrow().getPosterUrl());
    }

    @Test
    void posterEndpointEnforcesAuthorizationAndDoesNotUploadForMissingMovie() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "poster.png", "image/png", new byte[] {1});
        mockMvc.perform(multipart("/api/v1/admin/movies/{id}/poster", 999999L).file(file))
                .andExpect(status().isUnauthorized());
        for (String role : new String[] {"MEMBER", "EMPLOYEE"}) {
            mockMvc.perform(multipart("/api/v1/admin/movies/{id}/poster", 999999L).file(file)
                            .header("Authorization", bearer(role)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(multipart("/api/v1/admin/movies/{id}/poster", 999999L).file(file)
                        .header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(cloudinaryPosterService);
    }

    @Test
    void adminCanNormalizeReplaceAndClearTrailerWhilePublicDetailRetainsIt() throws Exception {
        Movie movie = movie();
        String admin = bearer("ADMIN");
        mockMvc.perform(patch("/api/v1/admin/movies/{id}/trailer", movie.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trailerUrl\":\"https://youtu.be/abcdefghijk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trailerUrl").value("https://www.youtube.com/watch?v=abcdefghijk"));
        mockMvc.perform(get("/api/v1/movies/{id}", movie.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trailerUrl").value("https://www.youtube.com/watch?v=abcdefghijk"));
        mockMvc.perform(patch("/api/v1/admin/movies/{id}/trailer", movie.getId()).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"trailerUrl\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.trailerUrl").doesNotExist());
    }

    @Test
    void trailerEndpointRejectsUnauthorizedMalformedAndOmittedValues() throws Exception {
        Movie movie = movie();
        String body = "{\"trailerUrl\":\"https://youtube.com/watch?v=abcdefghijk\"}";
        mockMvc.perform(patch("/api/v1/admin/movies/{id}/trailer", movie.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        for (String role : new String[] {"MEMBER", "EMPLOYEE"}) {
            mockMvc.perform(patch("/api/v1/admin/movies/{id}/trailer", movie.getId()).header("Authorization", bearer(role))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isForbidden());
        }
        for (String invalid : new String[] {
                "{}", "{\"trailerUrl\":\"http://youtube.com/watch?v=abcdefghijk\"}",
                "{\"trailerUrl\":\"https://youtube.com/watch?v=abcdefghijk&next=x\"}",
                "{\"trailerUrl\":\"<iframe src='x'>\"}"}) {
            mockMvc.perform(patch("/api/v1/admin/movies/{id}/trailer", movie.getId()).header("Authorization", bearer("ADMIN"))
                            .contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
    }

    private Movie movie() {
        Movie movie = new Movie();
        movie.setTitle("Media movie"); movie.setGenre("Hành động"); movie.setDurationMinutes(100);
        movie.setVersion("2D"); movie.setStatus("COMING_SOON");
        return movieRepository.saveAndFlush(movie);
    }

    private String bearer(String roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseGet(() -> {
            Role created = new Role(); created.setCode(roleCode); created.setName(roleCode); return roleRepository.save(created);
        });
        User user = new User(); user.setUsername("slice3-" + UUID.randomUUID()); user.setPasswordHash("hash");
        user.setFullName("Slice 3"); user.setStatus(UserStatus.ACTIVE); user.setRole(role);
        return "Bearer " + jwtService.generateAccessToken(userRepository.saveAndFlush(user));
    }

    @TestConfiguration
    static class MockPosterStorage {
        @Bean
        @Primary
        CloudinaryPosterService cloudinaryPosterService() {
            return Mockito.mock(CloudinaryPosterService.class);
        }
    }
}
