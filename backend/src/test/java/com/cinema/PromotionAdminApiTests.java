package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class PromotionAdminApiTests {

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
    void promotionAdministrationRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/promotions"))
                .andExpect(status().isUnauthorized());

        User employee = saveUser("promotion-employee", "EMPLOYEE");
        mockMvc.perform(get("/api/v1/admin/promotions")
                        .header("Authorization", bearer(employee)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateReadAndUpdatePromotion() throws Exception {
        User admin = saveUser("promotion-admin", "ADMIN");

        MvcResult created = mockMvc.perform(post("/api/v1/admin/promotions")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("Khai trương", "PERCENT", "20", "ACTIVE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Khai trương"))
                .andExpect(jsonPath("$.data.discountValue").value(20))
                .andReturn();

        Long id = promotionRepository.findAll().getFirst().getId();
        mockMvc.perform(get("/api/v1/admin/promotions/{id}", id)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/v1/admin/promotions/{id}", id)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("Giảm giá cuối tuần", "FIXED", "50000", "INACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Giảm giá cuối tuần"))
                .andExpect(jsonPath("$.data.discountType").value("FIXED"))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/admin/promotions")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void promotionAdministrationValidatesRequestAndReturnsNotFound() throws Exception {
        User admin = saveUser("promotion-validation-admin", "ADMIN");

        mockMvc.perform(post("/api/v1/admin/promotions")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("Không hợp lệ", "PERCENT", "101", "ACTIVE")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/admin/promotions")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Khoảng thời gian sai",
                                  "discountType": "FIXED",
                                  "discountValue": 10000,
                                  "startsAt": "2026-12-02T10:00:00Z",
                                  "endsAt": "2026-12-02T10:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/admin/promotions/{id}", 999999L)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/admin/promotions/{id}", 999999L)
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("Không tồn tại", "FIXED", "10000", "ACTIVE")))
                .andExpect(status().isNotFound());
    }

    private String bearer(User user) {
        return "Bearer " + jwtService.generateAccessToken(user);
    }

    private User saveUser(String username, String roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseGet(() -> {
            Role created = new Role();
            created.setCode(roleCode);
            created.setName(roleCode);
            return roleRepository.save(created);
        });
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("test-password-hash");
        user.setFullName(username);
        user.setStatus(UserStatus.ACTIVE);
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private String validRequest(String title, String discountType, String discountValue, String status) {
        return """
                {
                  "title": "%s",
                  "description": "Khuyến mãi kiểm thử",
                  "discountType": "%s",
                  "discountValue": %s,
                  "startsAt": "2026-12-01T10:00:00Z",
                  "endsAt": "2026-12-31T10:00:00Z",
                  "status": "%s"
                }
                """.formatted(title, discountType, discountValue, status);
    }
}
