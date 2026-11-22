package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Movie;
import com.cinema.entity.Booking;
import com.cinema.entity.BookingSeat;
import com.cinema.entity.Member;
import com.cinema.entity.Seat;
import com.cinema.entity.Ticket;
import com.cinema.entity.Payment;
import com.cinema.entity.Showtime;
import com.cinema.entity.Role;
import com.cinema.entity.Room;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.RoomRepository;
import com.cinema.repository.UserRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.security.JwtService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminShowtimeApiTests {
    private static final ZoneId CINEMA_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private MovieRepository movieRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private EntityManager entityManager;
    @Autowired private ShowtimeRepository showtimeRepository;
    private MockMvc mockMvc;
    private Movie movie;
    private Room activeRoom;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        movie = movie("NOW_SHOWING", 90);
        activeRoom = room("ACTIVE");
    }

    @Test
    void adminCanCreateListUpdateAndCancelFutureScheduledShowtime() throws Exception {
        LocalDate day = LocalDate.now(CINEMA_ZONE).plusDays(2);
        String create = request(movie.getId(), activeRoom.getId(), day + "T10:00:00+07:00");
        String admin = bearer("ADMIN");
        String created = mockMvc.perform(post("/api/v1/admin/showtimes").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(create))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.data.durationMinutes").value(90)).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(created.replaceFirst("(?s).*\\\"id\\\":(\\d+).*", "$1"));
        mockMvc.perform(get("/api/v1/admin/showtimes").header("Authorization", admin).param("date", day.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1));
        mockMvc.perform(patch("/api/v1/admin/showtimes/{id}", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(requestPatch(activeRoom.getId(), day + "T12:00:00+07:00")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.startsAt").exists());
        mockMvc.perform(post("/api/v1/admin/showtimes/{id}/cancel", id).header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void enforcesAdminAndStrictScheduleValidation() throws Exception {
        LocalDate day = LocalDate.now(CINEMA_ZONE).plusDays(2);
        String body = request(movie.getId(), activeRoom.getId(), day + "T10:00:00+07:00");
        mockMvc.perform(post("/api/v1/admin/showtimes").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        for (String role : java.util.List.of("MEMBER", "EMPLOYEE")) {
            mockMvc.perform(post("/api/v1/admin/showtimes").header("Authorization", bearer(role))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        }
        String admin = bearer("ADMIN");
        for (String invalid : java.util.List.of(
                "{\"movieId\":" + movie.getId() + ",\"roomId\":" + activeRoom.getId() + ",\"startsAt\":\"" + day + "T10:01:00+07:00\"}",
                "{\"movieId\":" + movie.getId() + ",\"roomId\":" + activeRoom.getId() + ",\"startsAt\":\"" + day + "T07:45:00+07:00\"}",
                "{\"movieId\":" + movie.getId() + ",\"roomId\":" + activeRoom.getId() + ",\"startsAt\":\"" + day + "T10:00:00+07:00\",\"status\":\"SCHEDULED\"}")) {
            mockMvc.perform(post("/api/v1/admin/showtimes").header("Authorization", admin)
                    .contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void enforcesHcmBoundariesAndStatusLifecycle() throws Exception {
        String admin = bearer("ADMIN"); LocalDate day = LocalDate.now(CINEMA_ZONE).plusDays(3);
        Movie hourMovie = movie("NOW_SHOWING", 60);
        mockMvc.perform(post("/api/v1/admin/showtimes").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(request(hourMovie.getId(), activeRoom.getId(), day + "T08:00:00+07:00")))
                .andExpect(status().isCreated());
        Movie lateMovie = movie("NOW_SHOWING", 60);
        mockMvc.perform(post("/api/v1/admin/showtimes").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(request(lateMovie.getId(), activeRoom.getId(), day + "T22:00:00+07:00")))
                .andExpect(status().isCreated());
        for (String time : java.util.List.of("T07:45:00+07:00", "T23:00:00+07:00", "T22:15:00+07:00", "T10:00:01+07:00", "T10:00:00")) {
            mockMvc.perform(post("/api/v1/admin/showtimes").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                    .content(request(movie("NOW_SHOWING", 60).getId(), activeRoom.getId(), day + time))).andExpect(status().isBadRequest());
        }
        Room inactive = room("INACTIVE");
        mockMvc.perform(post("/api/v1/admin/showtimes").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(request(movie("NOW_SHOWING", 60).getId(), inactive.getId(), day + "T12:00:00+07:00"))).andExpect(status().isBadRequest());
        String response = mockMvc.perform(post("/api/v1/admin/showtimes").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(request(movie("NOW_SHOWING", 60).getId(), activeRoom.getId(), day + "T14:00:00+07:00"))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(response.replaceFirst("(?s).*\\\"id\\\":(\\d+).*", "$1"));
        mockMvc.perform(patch("/api/v1/admin/showtimes/{id}", id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/admin/showtimes/{id}/cancel", id).header("Authorization", admin)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/showtimes/{id}/cancel", id).header("Authorization", admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("SHOWTIME_NOT_EDITABLE"));
    }

    @Test
    void bookingAndIndependentBookingSeatHistoryBlockPatchAndCancelWithoutMutation() throws Exception {
        String admin = bearer("ADMIN"); LocalDate day = LocalDate.now(CINEMA_ZONE).plusDays(5);
        long directBooking = createShowtimeId(admin, movie("NOW_SHOWING", 60), activeRoom, day + "T10:00:00+07:00");
        insertBooking(directBooking, "CANCELLED");
        assertHistoryBlocksBoth(admin, directBooking, activeRoom.getId(), day + "T11:00:00+07:00");

        long target = createShowtimeId(admin, movie("NOW_SHOWING", 60), activeRoom, day + "T13:00:00+07:00");
        long parent = createShowtimeId(admin, movie("NOW_SHOWING", 60), activeRoom, day + "T15:00:00+07:00");
        long bookingId = insertBooking(parent, "EXPIRED");
        Seat seat = new Seat(); seat.setRoom(activeRoom); seat.setRowLabel("Z"); seat.setSeatNumber(99); seat.setSeatType("STANDARD"); seat.setStatus("ACTIVE"); entityManager.persist(seat);
        BookingSeat bookingSeat = new BookingSeat(); bookingSeat.setBooking(entityManager.find(Booking.class, bookingId)); bookingSeat.setShowtime(showtimeRepository.findById(target).orElseThrow()); bookingSeat.setSeat(seat); bookingSeat.setUnitPrice(java.math.BigDecimal.ZERO); bookingSeat.setStatus("CANCELLED"); entityManager.persist(bookingSeat); entityManager.flush();
        assertHistoryBlocksBoth(admin, target, activeRoom.getId(), day + "T16:30:00+07:00");
    }

    @Test
    void ticketAndPaymentLinkedHistoryBlockAdminMutations() throws Exception {
        String admin = bearer("ADMIN"); LocalDate day = LocalDate.now(CINEMA_ZONE).plusDays(6);
        long paymentTarget = createShowtimeId(admin, movie("NOW_SHOWING", 60), activeRoom, day + "T10:00:00+07:00");
        long paymentBooking = insertBooking(paymentTarget, "PAID");
        Payment payment = new Payment(); payment.setBooking(entityManager.find(Booking.class, paymentBooking)); payment.setMethod("CARD"); payment.setAmount(java.math.BigDecimal.ZERO); payment.setStatus("PAID"); entityManager.persist(payment); entityManager.flush();
        assertHistoryBlocksBoth(admin, paymentTarget, activeRoom.getId(), day + "T11:30:00+07:00");

        long ticketTarget = createShowtimeId(admin, movie("NOW_SHOWING", 60), activeRoom, day + "T13:00:00+07:00");
        long parent = createShowtimeId(admin, movie("NOW_SHOWING", 60), activeRoom, day + "T15:00:00+07:00");
        long bookingId = insertBooking(parent, "PAID");
        Seat seat = new Seat(); seat.setRoom(activeRoom); seat.setRowLabel("Y"); seat.setSeatNumber(98); seat.setSeatType("STANDARD"); seat.setStatus("ACTIVE"); entityManager.persist(seat);
        BookingSeat bookingSeat = new BookingSeat(); bookingSeat.setBooking(entityManager.find(Booking.class, bookingId)); bookingSeat.setShowtime(showtimeRepository.findById(ticketTarget).orElseThrow()); bookingSeat.setSeat(seat); bookingSeat.setUnitPrice(java.math.BigDecimal.ZERO); bookingSeat.setStatus("CONFIRMED"); entityManager.persist(bookingSeat);
        Ticket ticket = new Ticket(); ticket.setBookingSeat(bookingSeat); ticket.setTicketCode("T" + UUID.randomUUID().toString().substring(0, 8)); ticket.setStatus("ISSUED"); entityManager.persist(ticket); entityManager.flush();
        assertHistoryBlocksBoth(admin, ticketTarget, activeRoom.getId(), day + "T16:30:00+07:00");
    }

    @Test
    void rejectsAllNonEditableShowtimeStates() throws Exception {
        String admin = bearer("ADMIN"); LocalDate day = LocalDate.now(CINEMA_ZONE).plusDays(7);
        for (String state : java.util.List.of("ONGOING", "FINISHED", "CANCELLED")) {
            long id = createShowtimeId(admin, movie("NOW_SHOWING", 60), activeRoom, day + "T10:00:00+07:00");
            Showtime showtime = showtimeRepository.findById(id).orElseThrow(); showtime.setStatus(state); showtimeRepository.saveAndFlush(showtime);
            assertNotEditable(admin, id, activeRoom.getId(), day + "T12:00:00+07:00");
        }
        Showtime past = new Showtime(); past.setMovie(movie("NOW_SHOWING", 60)); past.setRoom(activeRoom); past.setStartsAt(java.time.Instant.now().minusSeconds(3600)); past.setEndsAt(java.time.Instant.now().minusSeconds(60)); past.setStatus("SCHEDULED"); past = showtimeRepository.saveAndFlush(past);
        assertNotEditable(admin, past.getId(), activeRoom.getId(), day + "T12:00:00+07:00");
    }

    private void assertNotEditable(String admin, long id, Long roomId, String patchStart) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/showtimes/{id}", id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON).content(requestPatch(roomId, patchStart)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("SHOWTIME_NOT_EDITABLE"));
        mockMvc.perform(post("/api/v1/admin/showtimes/{id}/cancel", id).header("Authorization", admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("SHOWTIME_NOT_EDITABLE"));
    }

    private void assertHistoryBlocksBoth(String admin, long id, Long roomId, String patchStart) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/showtimes/{id}", id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(requestPatch(roomId, patchStart))).andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("SHOWTIME_HAS_BOOKING_HISTORY"));
        mockMvc.perform(post("/api/v1/admin/showtimes/{id}/cancel", id).header("Authorization", admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("SHOWTIME_HAS_BOOKING_HISTORY"));
    }

    private long createShowtimeId(String admin, Movie value, Room room, String startsAt) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/showtimes").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                .content(request(value.getId(), room.getId(), startsAt))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return Long.parseLong(response.replaceFirst("(?s).*\\\"id\\\":(\\d+).*", "$1"));
    }

    private long insertBooking(long showtimeId, String status) {
        Role memberRole = roleRepository.findByCode("MEMBER").orElseGet(() -> { Role value = new Role(); value.setCode("MEMBER"); value.setName("MEMBER"); return roleRepository.saveAndFlush(value); });
        User user = new User(); user.setUsername("history-" + UUID.randomUUID().toString().substring(0, 8)); user.setPasswordHash("hash"); user.setFullName("History"); user.setStatus(UserStatus.ACTIVE); user.setRole(memberRole); entityManager.persist(user);
        Member member = new Member(); member.setUser(user); member.setMemberCode("H" + UUID.randomUUID().toString().substring(0, 8)); entityManager.persist(member);
        Booking booking = new Booking(); booking.setBookingCode("B" + UUID.randomUUID().toString().substring(0, 8)); booking.setMember(member); booking.setShowtime(showtimeRepository.findById(showtimeId).orElseThrow()); booking.setStatus(status); booking.setSubtotal(java.math.BigDecimal.ZERO); booking.setDiscountAmount(java.math.BigDecimal.ZERO); booking.setTotalAmount(java.math.BigDecimal.ZERO); booking.setPointsUsed(0); entityManager.persist(booking); entityManager.flush(); return booking.getId();
    }

    private Movie movie(String status, int duration) {
        Movie value = new Movie(); value.setTitle("Admin Showtime " + UUID.randomUUID()); value.setGenre("Drama");
        value.setDurationMinutes(duration); value.setVersion("2D"); value.setStatus(status); return movieRepository.saveAndFlush(value);
    }
    private Room room(String status) {
        Room value = new Room(); value.setName("Admin room " + UUID.randomUUID()); value.setStatus(status); return roomRepository.saveAndFlush(value);
    }
    private String request(Long movieId, Long roomId, String startsAt) { return "{\"movieId\":" + movieId + ",\"roomId\":" + roomId + ",\"startsAt\":\"" + startsAt + "\"}"; }
    private String requestPatch(Long roomId, String startsAt) { return "{\"roomId\":" + roomId + ",\"startsAt\":\"" + startsAt + "\"}"; }
    private String bearer(String roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseGet(() -> { Role value = new Role(); value.setCode(roleCode); value.setName(roleCode); return roleRepository.save(value); });
        User user = new User(); user.setUsername("showtime-" + UUID.randomUUID()); user.setPasswordHash("hash"); user.setFullName("Showtime"); user.setStatus(UserStatus.ACTIVE); user.setRole(role);
        return "Bearer " + jwtService.generateAccessToken(userRepository.saveAndFlush(user));
    }
}
