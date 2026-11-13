package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.time.LocalDate;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AdminMovieUpdateApiTests {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private MovieRepository movieRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
        movieRepository.deleteAll();
    }

    @Test
    void adminCanUpdateMovieDetailsWithoutChangingMovieStatus() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Tên ban đầu");
        movie.setDurationMinutes(100);
        movie.setVersion("2D");
        movie.setStatus("STOPPED");
        movie.setReleaseDate(LocalDate.of(2025, 1, 1));
        movie = movieRepository.saveAndFlush(movie);

        String token = createToken("ADMIN");
        String request = """
                {
                  "title": "Tên phim mới",
                  "genre": "Kinh dị",
                  "releaseDate": "2026-12-01",
                  "durationMinutes": 125,
                  "version": "IMAX",
                  "castText": "Diễn viên A",
                  "director": "Đạo diễn B",
                  "studio": "Hãng phim C",
                  "synopsis": "Nội dung cập nhật",
                  "trailerUrl": "https://example.com/trailer",
                  "posterUrl": "https://example.com/poster.png"
                }
                """;

        mockMvc.perform(patch("/api/v1/admin/movies/{movieId}", movie.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Tên phim mới"))
                .andExpect(jsonPath("$.data.genre").value("Kinh dị"))
                .andExpect(jsonPath("$.data.durationMinutes").value(125))
                .andExpect(jsonPath("$.data.status").value("STOPPED"));

        Movie updated = movieRepository.findById(movie.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Tên phim mới", updated.getTitle());
        org.junit.jupiter.api.Assertions.assertEquals("STOPPED", updated.getStatus());
    }

    @Test
    void adminCanCreateMovieWithCloudinaryPosterUrl() throws Exception {
        String token = createToken("ADMIN");
        String request = """
                {
                  "title": "Phim tạo mới",
                  "genre": "Hành động",
                  "durationMinutes": 110,
                  "version": "2D",
                  "posterUrl": "https://res.cloudinary.com/demo/image/upload/cinema/posters/poster.png"
                }
                """;

        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Phim tạo mới"))
                .andExpect(jsonPath("$.data.posterUrl")
                        .value(org.hamcrest.Matchers.containsString("cloudinary.com")));

        org.junit.jupiter.api.Assertions.assertEquals(1, movieRepository.count());
    }

    @Test
    void nonAdminCannotUpdateMovie() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Phim không được sửa");
        movie.setDurationMinutes(90);
        movie.setVersion("2D");
        movie.setStatus("COMING_SOON");
        movie = movieRepository.saveAndFlush(movie);

        String token = createToken("EMPLOYEE");
        String request = """
                {
                  "title": "Không được phép",
                  "durationMinutes": 90,
                  "version": "2D"
                }
                """;

        mockMvc.perform(patch("/api/v1/admin/movies/{movieId}", movie.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSaveMovieWithoutChangingLegacyGenre() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Phim có thể loại cũ");
        movie.setGenre("Drama");
        movie.setDurationMinutes(90);
        movie.setVersion("2D");
        movie.setStatus("COMING_SOON");
        movie = movieRepository.saveAndFlush(movie);

        String token = createToken("ADMIN");
        String request = """
                {
                  "title": "Phim đã cập nhật",
                  "genre": "Drama",
                  "durationMinutes": 95,
                  "version": "2D"
                }
                """;

        mockMvc.perform(patch("/api/v1/admin/movies/{movieId}", movie.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.genre").value("Drama"));
    }

    @Test
    void posterUploadRequiresAdminRole() throws Exception {
        String token = createToken("EMPLOYEE");
        MockMultipartFile file = new MockMultipartFile(
                "file", "poster.png", "image/png", "not-a-png".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/movies/poster")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void posterUploadReportsMissingFileAndUnsupportedContentType() throws Exception {
        String token = createToken("ADMIN");

        mockMvc.perform(post("/api/v1/admin/movies/poster")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message")
                        .value("Yêu cầu upload phải dùng multipart/form-data"));

        mockMvc.perform(multipart("/api/v1/admin/movies/poster")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Backend chưa nhận được tệp poster. Hãy chọn ảnh và thử lại"));
    }

    @Test
    void posterUploadRejectsInvalidPngAndReportsMissingCloudinaryConfig() throws Exception {
        String token = createToken("ADMIN");
        MockMultipartFile invalidImage = new MockMultipartFile(
                "file", "poster.png", "image/png", "not-a-png".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/movies/poster")
                        .file(invalidImage)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tệp đã chọn không phải ảnh PNG hợp lệ"));

        ByteArrayOutputStream pngBytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), "png", pngBytes);
        MockMultipartFile png = new MockMultipartFile(
                "file", "poster.png", "image/png", pngBytes.toByteArray());
        mockMvc.perform(multipart("/api/v1/admin/movies/poster")
                        .file(png)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Cloudinary chưa được cấu hình")));
    }

    private String createToken(String roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseGet(() -> {
            Role newRole = new Role();
            newRole.setCode(roleCode);
            newRole.setName(roleCode);
            return roleRepository.save(newRole);
        });

        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername("movie-update-" + suffix);
        user.setPasswordHash("test-password-hash");
        user.setFullName("Movie Update Test");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        user = userRepository.saveAndFlush(user);
        return jwtService.generateAccessToken(user);
    }
}
