package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Banner;
import com.cinema.entity.Movie;
import com.cinema.entity.Promotion;
import com.cinema.repository.BannerRepository;
import com.cinema.repository.MovieRepository;
import com.cinema.repository.PromotionRepository;
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
class HomePublicApiTests {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private BannerRepository bannerRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private PromotionRepository promotionRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        bannerRepository.deleteAll();
        movieRepository.deleteAll();
        promotionRepository.deleteAll();
    }

    @Test
    void anonymousHomeReturnsCurrentEnvelopeAndControlledCatalogData() throws Exception {
        Banner banner = new Banner();
        banner.setSourceKey("home-test-banner"); banner.setTitle("Home banner"); banner.setSubtitle("Subtitle");
        banner.setImageUrl("https://example.test/banner.jpg"); banner.setTargetPath("/movies/1");
        banner.setDisplayOrder(0); banner.setActive(true); bannerRepository.saveAndFlush(banner);
        movieRepository.saveAndFlush(movie("Now showing", "NOW_SHOWING"));
        movieRepository.saveAndFlush(movie("Coming soon", "COMING_SOON"));
        Promotion promotion = new Promotion();
        promotion.setTitle("Home promotion"); promotion.setDiscountType("PERCENT"); promotion.setDiscountValue(BigDecimal.TEN);
        promotion.setStatus("ACTIVE"); promotion.setStartsAt(Instant.now().minusSeconds(60));
        promotion.setEndsAt(Instant.now().plusSeconds(60)); promotionRepository.saveAndFlush(promotion);

        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.banners[0].title").value("Home banner"))
                .andExpect(jsonPath("$.data.banners[0].imageUrl").value("https://example.test/banner.jpg"))
                .andExpect(jsonPath("$.data.nowShowing[0].title").value("Now showing"))
                .andExpect(jsonPath("$.data.nowShowing[0].status").value("NOW_SHOWING"))
                .andExpect(jsonPath("$.data.comingSoon[0].title").value("Coming soon"))
                .andExpect(jsonPath("$.data.comingSoon[0].status").value("COMING_SOON"))
                .andExpect(jsonPath("$.data.promotions[0].title").value("Home promotion"))
                .andExpect(jsonPath("$.data.showtimes").isArray());
    }

    @Test
    void anonymousHomeReturnsEmptyCollectionsWithoutServerError() throws Exception {
        mockMvc.perform(get("/api/v1/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.banners").isEmpty())
                .andExpect(jsonPath("$.data.nowShowing").isEmpty())
                .andExpect(jsonPath("$.data.comingSoon").isEmpty())
                .andExpect(jsonPath("$.data.showtimes").isEmpty())
                .andExpect(jsonPath("$.data.promotions").isEmpty());
    }

    private Movie movie(String title, String status) {
        Movie movie = new Movie(); movie.setTitle(title); movie.setGenre("Hành động");
        movie.setDurationMinutes(100); movie.setVersion("2D"); movie.setStatus(status);
        movie.setReleaseDate(LocalDate.of(2027, 1, 1)); return movie;
    }
}
