package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Role;
import com.cinema.entity.SeatPrice;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.SeatPriceAuditLogRepository;
import com.cinema.repository.SeatPriceRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class SeatPriceAdminApiTests {
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private SeatPriceRepository seatPriceRepository;
    @Autowired private SeatPriceAuditLogRepository auditLogRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        auditLogRepository.deleteAll();
        seatPriceRepository.deleteAll();
    }

    @Test
    void priceAdministrationRequiresAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/seat-prices")).andExpect(status().isUnauthorized());
        for (String role : new String[] {"MEMBER", "EMPLOYEE"}) {
            mockMvc.perform(get("/api/v1/admin/seat-prices").header("Authorization", bearer(saveUser("price-" + role, role))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void legacyPriceRulesCannotBePatchedOrActivated() throws Exception {
        User admin = saveUser("price-legacy-admin", "ADMIN");
        for (String origin : new String[] {"LEGACY_UNREVIEWED", "LOCAL_FIXTURE"}) {
            SeatPrice legacy = new SeatPrice();
            legacy.setSeatType("STANDARD");
            legacy.setPrice(new BigDecimal("90000"));
            legacy.setValidFrom(LocalDate.of(2027, 1, 1));
            legacy.setStatus("INACTIVE");
            legacy.setOrigin(origin);
            SeatPrice saved = seatPriceRepository.saveAndFlush(legacy);

            mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", saved.getId())
                            .header("Authorization", bearer(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"rowVersion\":" + saved.getRowVersion() + ",\"status\":\"ACTIVE\",\"amount\":100000}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Không tìm thấy quy tắc giá"));

            SeatPrice unchanged = seatPriceRepository.findById(saved.getId()).orElseThrow();
            assertEquals(origin, unchanged.getOrigin());
            assertEquals("INACTIVE", unchanged.getStatus());
            assertEquals(0, unchanged.getPrice().compareTo(new BigDecimal("90000")));
            assertEquals(saved.getRowVersion(), unchanged.getRowVersion());
            assertEquals(0, auditLogRepository.count());
        }
    }

    @Test
    void adminCreatesDraftActivatesRevisesAndPublicCatalogHidesFixture() throws Exception {
        User admin = saveUser("price-admin", "ADMIN");
        SeatPrice fixture = new SeatPrice();
        fixture.setSeatType("STANDARD"); fixture.setPrice(new BigDecimal("90000")); fixture.setValidFrom(LocalDate.now().minusDays(1));
        fixture.setStatus("ACTIVE"); fixture.setOrigin("LOCAL_FIXTURE");
        seatPriceRepository.saveAndFlush(fixture);

        mockMvc.perform(get("/api/v1/ticket-prices"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(post("/api/v1/admin/seat-prices").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(request("VIP", "IMAX   Laser", "150000", "2026-12-01", null)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.origin").value("ADMIN_APPROVED"));
        SeatPrice draft = seatPriceRepository.findAll().stream().filter(rule -> "DRAFT".equals(rule.getStatus())).findFirst().orElseThrow();

        mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", draft.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rowVersion\":" + draft.getRowVersion() + ",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ACTIVE"));
        SeatPrice active = seatPriceRepository.findById(draft.getId()).orElseThrow();

        mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", active.getId()).header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"rowVersion": %d, "amount": 160000, "seatType": "VIP", "movieVersion": "imax laser", "validFrom": "2026-12-01"}
                                """.formatted(active.getRowVersion())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.amount").value(160000))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/admin/seat-prices").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/v1/ticket-prices").param("date", "2026-12-02"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].price").value(160000));
    }

    private String request(String seatType, String movieVersion, String amount, String validFrom, String validTo) {
        return """
                {"seatType":"%s","movieVersion":"%s","amount":%s,"validFrom":"%s","validTo":%s}
                """.formatted(seatType, movieVersion, amount, validFrom, validTo == null ? "null" : "\"" + validTo + "\"");
    }

    private String bearer(User user) { return "Bearer " + jwtService.generateAccessToken(user); }

    private User saveUser(String username, String roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseGet(() -> {
            Role created = new Role(); created.setCode(roleCode); created.setName(roleCode); return roleRepository.save(created);
        });
        User user = new User(); user.setUsername(username); user.setPasswordHash("test-password-hash");
        user.setFullName(username); user.setStatus(UserStatus.ACTIVE); user.setRole(role);
        return userRepository.saveAndFlush(user);
    }
}
