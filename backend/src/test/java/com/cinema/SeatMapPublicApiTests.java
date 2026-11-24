package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cinema.entity.Booking;
import com.cinema.entity.BookingSeat;
import com.cinema.entity.Member;
import com.cinema.entity.Movie;
import com.cinema.entity.Role;
import com.cinema.entity.Room;
import com.cinema.entity.Seat;
import com.cinema.entity.Showtime;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.BookingSeatRepository;
import com.cinema.repository.ShowtimeRepository;
import com.cinema.security.JwtService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
class SeatMapPublicApiTests {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private EntityManager entityManager;
    @Autowired private ShowtimeRepository showtimeRepository;
    @Autowired private BookingSeatRepository bookingSeatRepository;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void publicSeatMapUsesRoomSeatsAndDoesNotExposeHolderData() throws Exception {
        Fixture fixture = createFixture(Instant.now().plus(Duration.ofHours(2)), "SCHEDULED");
        fixture.unavailable.setStatus("UNAVAILABLE");
        entityManager.flush();
        createBookingSeat(fixture, fixture.held, "HELD", Instant.now().plus(Duration.ofMinutes(10)));
        createBookingSeat(fixture, fixture.sold, "CONFIRMED", null);

        mockMvc.perform(get("/api/v1/showtimes/{showtimeId}/seat-map", fixture.showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.showtime.id").value(fixture.showtime.getId()))
                .andExpect(jsonPath("$.data.showtime.roomName").value(fixture.room.getName()))
                .andExpect(jsonPath("$.data.serverTime").isNotEmpty())
                .andExpect(jsonPath("$.data.salesOpen").value(true))
                .andExpect(jsonPath("$.data.seats.length()").value(4))
                .andExpect(jsonPath("$.data.seats[0].availability").value("AVAILABLE"))
                .andExpect(jsonPath("$.data.seats[1].availability").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data.seats[2].availability").value("HELD"))
                .andExpect(jsonPath("$.data.seats[2].username").doesNotExist())
                .andExpect(jsonPath("$.data.seats[2].memberId").doesNotExist())
                .andExpect(jsonPath("$.data.seats[3].availability").value("SOLD"));
    }

    @Test
    void memberOnlySeesOwnHeldForItsOwnActiveHold() throws Exception {
        Fixture fixture = createFixture(Instant.now().plus(Duration.ofHours(2)), "SCHEDULED");
        User holder = createBookingSeat(fixture, fixture.held, "HELD", Instant.now().plus(Duration.ofMinutes(10)));

        mockMvc.perform(get("/api/v1/showtimes/{showtimeId}/seat-map", fixture.showtime.getId())
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(holder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats[2].availability").value("OWN_HELD"));
    }

    @Test
    void nonMemberTokenCannotReceiveOwnHeldState() throws Exception {
        Fixture fixture = createFixture(Instant.now().plus(Duration.ofHours(2)), "SCHEDULED");
        createBookingSeat(fixture, fixture.held, "HELD", Instant.now().plus(Duration.ofMinutes(10)));
        User employee = createUser("EMPLOYEE");

        mockMvc.perform(get("/api/v1/showtimes/{showtimeId}/seat-map", fixture.showtime.getId())
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats[2].availability").value("HELD"));
    }

    @Test
    void expiredHoldIsAvailableAndSeatMapDoesNotChangeBookingRows() throws Exception {
        Fixture fixture = createFixture(Instant.now().plus(Duration.ofHours(2)), "SCHEDULED");
        createBookingSeat(fixture, fixture.held, "HELD", Instant.now().minusSeconds(1));

        mockMvc.perform(get("/api/v1/showtimes/{showtimeId}/seat-map", fixture.showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats[2].availability").value("AVAILABLE"));

        BookingSeat stored = bookingSeatRepository.findAll().getFirst();
        assertEquals("HELD", stored.getStatus());
        assertEquals("PENDING_PAYMENT", stored.getBooking().getStatus());
    }

    @Test
    void seatMapContainsOnlySeatsFromTheShowtimeRoom() throws Exception {
        Fixture fixture = createFixture(Instant.now().plus(Duration.ofHours(2)), "SCHEDULED");
        Room otherRoom = new Room();
        otherRoom.setName("Other room " + System.nanoTime());
        otherRoom.setStatus("ACTIVE");
        entityManager.persist(otherRoom);
        Seat otherRoomSeat = seat(otherRoom, 1);
        entityManager.flush();

        mockMvc.perform(get("/api/v1/showtimes/{showtimeId}/seat-map", fixture.showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats.length()").value(4))
                .andExpect(jsonPath("$.data.seats[?(@.id == %s)]", otherRoomSeat.getId()).isEmpty());
    }

    @Test
    void startedOrCancelledShowtimeIsReturnedReadOnlyButNotForSale() throws Exception {
        Fixture started = createFixture(Instant.now().plus(Duration.ofMinutes(20)), "SCHEDULED");
        mockMvc.perform(get("/api/v1/showtimes/{showtimeId}/seat-map", started.showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.salesOpen").value(false))
                .andExpect(jsonPath("$.data.seats[0].availability").value("NOT_FOR_SALE"));

        Fixture cancelled = createFixture(Instant.now().plus(Duration.ofHours(2)), "CANCELLED");
        mockMvc.perform(get("/api/v1/showtimes/{showtimeId}/seat-map", cancelled.showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.salesOpen").value(false));
    }

    @Test
    void unknownShowtimeReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/showtimes/{showtimeId}/seat-map", 999999L))
                .andExpect(status().isNotFound());
    }

    private Fixture createFixture(Instant startsAt, String showtimeStatus) {
        Movie movie = new Movie();
        movie.setTitle("Seat map movie " + System.nanoTime());
        movie.setDurationMinutes(120);
        movie.setVersion("2D");
        movie.setStatus("NOW_SHOWING");
        movie.setReleaseDate(LocalDate.now());
        entityManager.persist(movie);
        Room room = new Room();
        room.setName("Seat map room " + System.nanoTime());
        room.setStatus("ACTIVE");
        entityManager.persist(room);
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartsAt(startsAt);
        showtime.setEndsAt(startsAt.plus(Duration.ofMinutes(120)));
        showtime.setStatus(showtimeStatus);
        showtime = showtimeRepository.saveAndFlush(showtime);
        Seat available = seat(room, 1);
        Seat unavailable = seat(room, 2);
        Seat held = seat(room, 3);
        Seat sold = seat(room, 4);
        entityManager.flush();
        return new Fixture(room, showtime, available, unavailable, held, sold);
    }

    private Seat seat(Room room, int number) {
        Seat seat = new Seat();
        seat.setRoom(room);
        seat.setRowLabel("A");
        seat.setSeatNumber(number);
        seat.setSeatType(number % 2 == 0 ? "VIP" : "STANDARD");
        seat.setStatus("AVAILABLE");
        entityManager.persist(seat);
        return seat;
    }

    private User createBookingSeat(Fixture fixture, Seat seat, String seatStatus, Instant expiresAt) {
        User user = createUser("MEMBER");
        Member member = new Member();
        member.setUser(user);
        member.setMemberCode("MB" + System.nanoTime());
        entityManager.persist(member);
        Booking booking = new Booking();
        booking.setBookingCode("SM" + System.nanoTime());
        booking.setMember(member);
        booking.setShowtime(fixture.showtime);
        booking.setStatus(seatStatus.equals("HELD") ? "PENDING_PAYMENT" : "PAID");
        booking.setSubtotal(BigDecimal.ZERO);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setPointsUsed(0);
        booking.setTotalAmount(BigDecimal.ZERO);
        booking.setExpiresAt(expiresAt);
        entityManager.persist(booking);
        BookingSeat bookingSeat = new BookingSeat();
        bookingSeat.setBooking(booking);
        bookingSeat.setShowtime(fixture.showtime);
        bookingSeat.setSeat(seat);
        bookingSeat.setUnitPrice(BigDecimal.ZERO);
        bookingSeat.setStatus(seatStatus);
        bookingSeatRepository.saveAndFlush(bookingSeat);
        return user;
    }

    private User createUser(String roleCode) {
        Role role = entityManager.createQuery(
                        "select role from Role role where role.code = :roleCode", Role.class)
                .setParameter("roleCode", roleCode)
                .getResultStream().findFirst().orElse(null);
        if (role == null) {
            role = new Role();
            role.setCode(roleCode);
            role.setName(roleCode + " test");
            entityManager.persist(role);
        }
        User user = new User();
        user.setUsername("seatmap-" + System.nanoTime());
        user.setPasswordHash("not-used");
        user.setFullName("Seat map holder");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        entityManager.persist(user);
        return user;
    }

    private record Fixture(Room room, Showtime showtime, Seat available, Seat unavailable, Seat held, Seat sold) {}
}
