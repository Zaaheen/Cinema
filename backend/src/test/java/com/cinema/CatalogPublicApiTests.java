package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Movie;
import com.cinema.entity.Promotion;
import com.cinema.entity.SeatPrice;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.PromotionRepository;
import com.cinema.repository.SeatPriceRepository;
import java.math.BigDecimal;
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
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class CatalogPublicApiTests {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private MovieRepository movieRepository;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private SeatPriceRepository seatPriceRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        movieRepository.deleteAll();
        promotionRepository.deleteAll();
        seatPriceRepository.deleteAll();
    }

    @Test
    void publicCatalogReturnsSearchableMoviesPublishedPromotionsAndEffectivePrices() throws Exception {
        Movie movie = new Movie();
        movie.setTitle("Dune: Part Two");
        movie.setGenre("Science fiction");
        movie.setDurationMinutes(166);
        movie.setVersion("IMAX");
        movie.setStatus("NOW_SHOWING");
        movie.setReleaseDate(LocalDate.now());
        movieRepository.save(movie);
        Movie comingSoon = new Movie();
        comingSoon.setTitle("Wicked");
        comingSoon.setDurationMinutes(160);
        comingSoon.setVersion("2D");
        comingSoon.setStatus("COMING_SOON");
        comingSoon.setReleaseDate(LocalDate.now().plusDays(7));
        movieRepository.save(comingSoon);
        Movie stopped = new Movie();
        stopped.setTitle("Archived movie");
        stopped.setDurationMinutes(90);
        stopped.setVersion("2D");
        stopped.setStatus("STOPPED");
        stopped.setReleaseDate(LocalDate.now().minusDays(30));
        movieRepository.save(stopped);

        Promotion active = new Promotion();
        active.setTitle("Ưu đãi thành viên");
        active.setDiscountType("PERCENT");
        active.setDiscountValue(BigDecimal.TEN);
        active.setStatus("ACTIVE");
        active.setStartsAt(Instant.now().minusSeconds(60));
        active.setEndsAt(Instant.now().plusSeconds(60));
        promotionRepository.save(active);
        Promotion expired = new Promotion();
        expired.setTitle("Đã hết hạn");
        expired.setDiscountType("FIXED");
        expired.setDiscountValue(BigDecimal.ONE);
        expired.setStatus("ACTIVE");
        expired.setStartsAt(Instant.now().minusSeconds(120));
        expired.setEndsAt(Instant.now().minusSeconds(60));
        promotionRepository.save(expired);
        Promotion inactive = new Promotion();
        inactive.setTitle("Chưa công bố");
        inactive.setDiscountType("FIXED");
        inactive.setDiscountValue(BigDecimal.ONE);
        inactive.setStatus("INACTIVE");
        inactive.setStartsAt(Instant.now().minusSeconds(60));
        inactive.setEndsAt(Instant.now().plusSeconds(60));
        promotionRepository.save(inactive);

        SeatPrice price = new SeatPrice();
        price.setSeatType("STANDARD");
        price.setPrice(new BigDecimal("75000.00"));
        price.setValidFrom(LocalDate.now().minusDays(1));
        seatPriceRepository.save(price);
        SeatPrice expiredPrice = new SeatPrice();
        expiredPrice.setSeatType("VIP");
        expiredPrice.setPrice(new BigDecimal("120000.00"));
        expiredPrice.setValidFrom(LocalDate.now().minusDays(2));
        expiredPrice.setValidTo(LocalDate.now().minusDays(1));
        seatPriceRepository.save(expiredPrice);

        mockMvc.perform(get("/api/v1/movies").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/v1/movies").param("status", "COMING_SOON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Wicked"));
        mockMvc.perform(get("/api/v1/movies").param("status", "STOPPED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(get("/api/v1/movies").param("q", "dune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Dune: Part Two"));
        mockMvc.perform(get("/api/v1/movies/{movieId}", movie.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.durationMinutes").value(166));
        mockMvc.perform(get("/api/v1/movies/{movieId}", 999999L))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Ưu đãi thành viên"));
        mockMvc.perform(get("/api/v1/ticket-prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].price").value(75000));
    }

    @Test
    void moviesWithoutSearchParametersReturnsAnEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }
}
