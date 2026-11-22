package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Promotion;
import com.cinema.entity.Role;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.PromotionRepository;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
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
class PublicPromotionDetailApiTests {

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private PromotionRepository promotionRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
        promotionRepository.deleteAll();
    }

    @Test
    void publicDetailOnlyExposesActivePromotionWithinItsValidityWindow() throws Exception {
        Instant now = Instant.now();
        Promotion active = savePromotion("Công khai", "ACTIVE", now.minusSeconds(3600), now.plusSeconds(3600));
        Promotion inactive = savePromotion("Không hoạt động", "INACTIVE", now.minusSeconds(3600), now.plusSeconds(3600));
        Promotion future = savePromotion("Chưa bắt đầu", "ACTIVE", now.plusSeconds(86_400), now.plusSeconds(172_800));
        Promotion expired = savePromotion("Đã hết hạn", "ACTIVE", now.minusSeconds(172_800), now.minusSeconds(86_400));

        mockMvc.perform(get("/api/v1/promotions/{id}", active.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(active.getId()))
                .andExpect(jsonPath("$.data.title").value("Công khai"));

        User member = saveUser("MEMBER");
        mockMvc.perform(get("/api/v1/promotions/{id}", active.getId())
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(active.getId()));

        mockMvc.perform(get("/api/v1/promotions/{id}", inactive.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/promotions/{id}", future.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/promotions/{id}", expired.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/promotions/{id}", 999_999L))
                .andExpect(status().isNotFound());
    }

    private Promotion savePromotion(String title, String status, Instant startsAt, Instant endsAt) {
        Promotion promotion = new Promotion();
        promotion.setTitle(title);
        promotion.setDescription("Promotion visibility test");
        promotion.setDiscountType("FIXED");
        promotion.setDiscountValue(BigDecimal.TEN);
        promotion.setStartsAt(startsAt);
        promotion.setEndsAt(endsAt);
        promotion.setStatus(status);
        return promotionRepository.saveAndFlush(promotion);
    }

    private User saveUser(String roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseGet(() -> {
            Role created = new Role();
            created.setCode(roleCode);
            created.setName(roleCode);
            return roleRepository.save(created);
        });
        String suffix = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setUsername("public-promotion-" + suffix);
        user.setPasswordHash("test-password-hash");
        user.setFullName("Public Promotion Test");
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }
}
