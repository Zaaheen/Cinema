package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Booking;
import com.cinema.entity.Genre;
import com.cinema.entity.Member;
import com.cinema.entity.Movie;
import com.cinema.entity.Role;
import com.cinema.entity.Room;
import com.cinema.entity.Showtime;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.BookingRepository;
import com.cinema.repository.GenreRepository;
import com.cinema.repository.MemberRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.RoomRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
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
class AdminShowtimeApiTests {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private MovieRepository movieRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;
    private Room roomA;
    private Room roomB;
    private Movie movie;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
        bookingRepository.deleteAll();
        showtimeRepository.deleteAll();
        movieRepository.deleteAll();
        roomRepository.deleteAll();
        genreRepository.deleteAll();
        genreRepository.saveAll(List.of(
                genre("Hành động"), genre("Hài kịch"), genre("Kinh dị"),
                genre("Khoa học viễn tưởng"), genre("Tình cảm"), genre("Phiêu lưu")));
        roomA = saveRoom("Phòng A");
        roomB = saveRoom("Phòng B");
        movie = saveMovie(120);
        adminToken = createToken("ADMIN");
    }

    @Test
    void adminCanCreateMovieWithManyGenresAndShowtimesInDifferentRooms() throws Exception {
        Long actionId = genreRepository.findAllBySelectableTrueOrderByNameAsc().stream()
                .filter(genre -> genre.getName().equals("Hành động"))
                .findFirst().orElseThrow().getId();
        Long horrorId = genreRepository.findAllBySelectableTrueOrderByNameAsc().stream()
                .filter(genre -> genre.getName().equals("Kinh dị"))
                .findFirst().orElseThrow().getId();
        String day = futureDate().toString();
        String firstStart = offsetDateTime(day, 9, 0);
        String secondStart = offsetDateTime(day, 9, 0);
        String request = """
                {
                  "title": "Phim nhiều thể loại",
                  "genreIds": [%d, %d],
                  "durationMinutes": 120,
                  "version": "2D",
                  "showtimes": [
                    {"roomId": %d, "startsAt": "%s"},
                    {"roomId": %d, "startsAt": "%s"}
                  ]
                }
                """.formatted(actionId, horrorId, roomA.getId(), firstStart, roomB.getId(), secondStart);

        mockMvc.perform(post("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.genre").value("Hành động, Kinh dị"))
                .andExpect(jsonPath("$.data.genres.length()").value(2));

        assertEquals(2, movieRepository.count());
        List<Showtime> saved = showtimeRepository.findAll();
        assertEquals(2, saved.size());
        assertEquals(120 * 60L, saved.get(0).getEndsAt().getEpochSecond() - saved.get(0).getStartsAt().getEpochSecond());
    }

    @Test
    void conflictingBatchRollsBackEveryShowtime() throws Exception {
        String day = futureDate().toString();
        String request = """
                {
                  "movieId": %d,
                  "showtimes": [
                    {"roomId": %d, "startsAt": "%s"},
                    {"roomId": %d, "startsAt": "%s"}
                  ]
                }
                """.formatted(movie.getId(), roomA.getId(), offsetDateTime(day, 9, 0),
                roomA.getId(), offsetDateTime(day, 11, 0));

        mockMvc.perform(post("/api/v1/admin/showtimes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());

        assertEquals(0, showtimeRepository.count());
    }

    @Test
    void roomCanScheduleAtExactlyFifteenMinuteBuffer() throws Exception {
        String day = futureDate().toString();
        String request = """
                {
                  "movieId": %d,
                  "showtimes": [
                    {"roomId": %d, "startsAt": "%s"},
                    {"roomId": %d, "startsAt": "%s"}
                  ]
                }
                """.formatted(movie.getId(), roomA.getId(), offsetDateTime(day, 9, 0),
                roomA.getId(), offsetDateTime(day, 11, 15));

        mockMvc.perform(post("/api/v1/admin/showtimes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void adminCanEditAndCancelUnbookedShowtime() throws Exception {
        Showtime showtime = createOneShowtime();
        String day = futureDate().toString();
        String update = """
                {"roomId": %d, "startsAt": "%s"}
                """.formatted(roomB.getId(), offsetDateTime(day, 10, 0));

        mockMvc.perform(patch("/api/v1/admin/showtimes/{id}", showtime.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(roomB.getId()));

        mockMvc.perform(delete("/api/v1/admin/showtimes/{id}", showtime.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
        assertEquals("CANCELLED", showtimeRepository.findById(showtime.getId()).orElseThrow().getStatus());
    }

    @Test
    void adminCannotEditOrCancelShowtimeThatHasBooking() throws Exception {
        Showtime showtime = createOneShowtime();
        Member member = createMember();
        Booking booking = new Booking();
        booking.setBookingCode("BK" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        booking.setMember(member);
        booking.setShowtime(showtime);
        booking.setStatus("PENDING_PAYMENT");
        booking.setSubtotal(BigDecimal.ZERO);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setPointsUsed(0);
        booking.setTotalAmount(BigDecimal.ZERO);
        bookingRepository.saveAndFlush(booking);

        String update = """
                {"roomId": %d, "startsAt": "%s"}
                """.formatted(roomB.getId(), offsetDateTime(futureDate().toString(), 10, 0));

        mockMvc.perform(patch("/api/v1/admin/showtimes/{id}", showtime.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isConflict());
        mockMvc.perform(delete("/api/v1/admin/showtimes/{id}", showtime.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/admin/movies/{id}", movie.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movieUpdateRequest(135)))
                .andExpect(status().isConflict());
        assertEquals(120, movieRepository.findById(movie.getId()).orElseThrow().getDurationMinutes());
    }

    @Test
    void changingMovieDurationRecalculatesScheduledEndTime() throws Exception {
        Showtime showtime = createOneShowtime();

        mockMvc.perform(patch("/api/v1/admin/movies/{id}", movie.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movieUpdateRequest(130)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.durationMinutes").value(130));

        Showtime updated = showtimeRepository.findById(showtime.getId()).orElseThrow();
        assertEquals(130 * 60L, updated.getEndsAt().getEpochSecond() - updated.getStartsAt().getEpochSecond());
    }

    @Test
    void changingMovieDurationRollsBackWhenItWouldConflictWithAnotherShowtime() throws Exception {
        createOneShowtime();
        Movie otherMovie = saveMovie(30);
        Showtime next = new Showtime();
        next.setMovie(otherMovie);
        next.setRoom(roomA);
        next.setStartsAt(OffsetDateTime.parse(offsetDateTime(futureDate().toString(), 11, 15)).toInstant());
        next.setEndsAt(next.getStartsAt().plusSeconds(30 * 60L));
        next.setStatus("SCHEDULED");
        showtimeRepository.saveAndFlush(next);

        mockMvc.perform(patch("/api/v1/admin/movies/{id}", movie.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(movieUpdateRequest(135)))
                .andExpect(status().isConflict());

        assertEquals(120, movieRepository.findById(movie.getId()).orElseThrow().getDurationMinutes());
    }

    @Test
    void employeeCannotAccessAdminRoomCalendar() throws Exception {
        String employeeToken = createToken("EMPLOYEE");
        mockMvc.perform(get("/api/v1/admin/rooms")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden());
    }

    private Showtime createOneShowtime() {
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(roomA);
        showtime.setStartsAt(OffsetDateTime.parse(offsetDateTime(futureDate().toString(), 9, 0)).toInstant());
        showtime.setEndsAt(showtime.getStartsAt().plusSeconds(120 * 60L));
        showtime.setStatus("SCHEDULED");
        return showtimeRepository.saveAndFlush(showtime);
    }

    private Movie saveMovie(int duration) {
        Movie item = new Movie();
        item.setTitle("Phim kiểm thử");
        item.setDurationMinutes(duration);
        item.setVersion("2D");
        item.setStatus("COMING_SOON");
        return movieRepository.saveAndFlush(item);
    }

    private String movieUpdateRequest(int duration) {
        return """
                {"title":"Phim kiểm thử","genreIds":[],"durationMinutes":%d,"version":"2D"}
                """.formatted(duration);
    }

    private Room saveRoom(String name) {
        Room room = new Room();
        room.setName(name);
        room.setStatus("ACTIVE");
        return roomRepository.saveAndFlush(room);
    }

    private Member createMember() {
        Role role = roleRepository.findByCode("MEMBER").orElseGet(() -> {
            Role newRole = new Role();
            newRole.setCode("MEMBER");
            newRole.setName("Member");
            return roleRepository.save(newRole);
        });
        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername("showtime-member-" + suffix);
        user.setPasswordHash("test-hash");
        user.setFullName("Showtime Member");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        user = userRepository.saveAndFlush(user);
        Member member = new Member();
        member.setUser(user);
        member.setMemberCode("MB" + suffix.substring(0, 8));
        return memberRepository.saveAndFlush(member);
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
        user.setUsername("showtime-test-" + suffix);
        user.setPasswordHash("test-hash");
        user.setFullName("Showtime Test");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        return jwtService.generateAccessToken(userRepository.saveAndFlush(user));
    }

    private Genre genre(String name) {
        Genre genre = new Genre();
        genre.setName(name);
        return genre;
    }

    private LocalDate futureDate() {
        return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(3);
    }

    private String offsetDateTime(String date, int hour, int minute) {
        return OffsetDateTime.of(LocalDate.parse(date), LocalTime.of(hour, minute), ZoneOffset.ofHours(7)).toString();
    }
}
