package com.cinema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.dto.price.SeatPriceRequest;
import com.cinema.dto.price.SeatPriceUpdateRequest;
import com.cinema.entity.Role;
import com.cinema.entity.SeatPrice;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.SeatPriceAuditLogRepository;
import com.cinema.repository.SeatPriceRepository;
import com.cinema.repository.UserRepository;
import com.cinema.service.SeatPriceAdminService;
import com.cinema.security.JwtService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.profiles.active=local")
@Testcontainers(disabledWithoutDocker = true)
class SeatPricePostgresServiceTests {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("cinema_service_test").withUsername("cinema").withPassword("cinema");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-key-for-postgres-service-tests-0123456789abcdef");
        registry.add("app.jwt.refresh-cookie-secure", () -> false);
    }

    @Autowired private SeatPriceAdminService seatPriceAdminService;
    @Autowired private SeatPriceRepository seatPriceRepository;
    @Autowired private SeatPriceAuditLogRepository auditLogRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private JwtService jwtService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(springSecurityFilterChain).build();
        auditLogRepository.deleteAll();
        seatPriceRepository.deleteAll();
        Role role = roleRepository.findByCode("ADMIN").orElseThrow();
        User admin = userRepository.findByUsername("postgres-price-admin").orElseGet(() -> {
            User created = new User(); created.setUsername("postgres-price-admin"); created.setPasswordHash("hash");
            created.setFullName("Postgres Price Admin"); created.setStatus(UserStatus.ACTIVE); created.setRole(role);
            return userRepository.saveAndFlush(created);
        });
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(admin.getUsername(), null,
                AuthorityUtils.createAuthorityList("ROLE_ADMIN")));
    }

    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test
    void writesAuditAndSuccessorRevisionAtomicallyOnPostgres() {
        var draft = seatPriceAdminService.createDraft(new SeatPriceRequest("STANDARD", null,
                new BigDecimal("100000"), LocalDate.of(2027, 1, 1), null));
        var active = seatPriceAdminService.update(draft.id(), new SeatPriceUpdateRequest(draft.rowVersion(),
                null, null, null, null, null, "ACTIVE"));
        var successor = seatPriceAdminService.update(active.id(), new SeatPriceUpdateRequest(active.rowVersion(),
                "STANDARD", null, new BigDecimal("110000"), LocalDate.of(2027, 1, 1), null, null));

        assertEquals("ACTIVE", successor.status());
        assertEquals(2, seatPriceRepository.count());
        assertEquals(4, auditLogRepository.count());
        assertTrue(seatPriceRepository.findAll().stream().anyMatch(rule -> "INACTIVE".equals(rule.getStatus())));
    }

    @Test
    void enforcesAdminRouteAuthorizationOnPostgres() throws Exception {
        mockMvc.perform(get("/api/v1/admin/seat-prices")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/seat-prices").header("Authorization", bearer(user("postgres-price-member", "MEMBER"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/seat-prices").header("Authorization", bearer(user("postgres-price-employee", "EMPLOYEE"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/seat-prices").header("Authorization", bearer(user("postgres-price-admin", "ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsLegacyPricePatchWithoutWritingAuditOnPostgres() throws Exception {
        User admin = user("postgres-price-admin", "ADMIN");
        SeatPrice legacy = new SeatPrice();
        legacy.setSeatType("STANDARD");
        legacy.setPrice(new BigDecimal("90000"));
        legacy.setValidFrom(LocalDate.of(2027, 1, 1));
        legacy.setStatus("INACTIVE");
        legacy.setOrigin("LEGACY_UNREVIEWED");
        SeatPrice saved = seatPriceRepository.saveAndFlush(legacy);

        mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", saved.getId())
                        .header("Authorization", bearer(admin)).contentType("application/json")
                        .content("{\"rowVersion\":" + saved.getRowVersion() + ",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNotFound());

        SeatPrice unchanged = seatPriceRepository.findById(saved.getId()).orElseThrow();
        assertEquals("LEGACY_UNREVIEWED", unchanged.getOrigin());
        assertEquals("INACTIVE", unchanged.getStatus());
        assertEquals(0, unchanged.getPrice().compareTo(new BigDecimal("90000")));
        assertEquals(saved.getRowVersion(), unchanged.getRowVersion());
        assertEquals(0, auditLogRepository.count());
    }

    @Test
    void patchResponseReturnsThePersistedOptimisticLockVersion() throws Exception {
        var draft = seatPriceAdminService.createDraft(new SeatPriceRequest("STANDARD", null,
                new BigDecimal("100000"), LocalDate.of(2027, 1, 1), null));
        User admin = user("postgres-price-admin", "ADMIN");

        mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", draft.id())
                        .header("Authorization", bearer(admin))
                        .contentType("application/json")
                        .content("{\"rowVersion\":" + draft.rowVersion() + ",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rowVersion").value(draft.rowVersion() + 1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertEquals(draft.rowVersion() + 1,
                seatPriceRepository.findById(draft.id()).orElseThrow().getRowVersion());
        assertEquals(2, auditLogRepository.count());
    }

    @Test
    void responseVersionCanDriveDeactivateAndRejectsStaleVersionWithoutDuplicateAudit() throws Exception {
        var draft = seatPriceAdminService.createDraft(new SeatPriceRequest("STANDARD", null,
                new BigDecimal("100000"), LocalDate.of(2027, 2, 1), null));
        User admin = user("postgres-price-admin", "ADMIN");
        String authorization = bearer(admin);

        mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", draft.id())
                        .header("Authorization", authorization).contentType("application/json")
                        .content("{\"rowVersion\":0,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.rowVersion").value(1));

        mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", draft.id())
                        .header("Authorization", authorization).contentType("application/json")
                        .content("{\"rowVersion\":1,\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.rowVersion").value(2))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", draft.id())
                        .header("Authorization", authorization).contentType("application/json")
                        .content("{\"rowVersion\":1,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict());

        assertEquals("INACTIVE", seatPriceRepository.findById(draft.id()).orElseThrow().getStatus());
        assertEquals(2L, seatPriceRepository.findById(draft.id()).orElseThrow().getRowVersion());
        assertEquals(3, auditLogRepository.count());
    }

    @Test
    void activeRevisionReturnsNewRuleVersionAndDoesNotDuplicateAudit() throws Exception {
        var draft = seatPriceAdminService.createDraft(new SeatPriceRequest("VIP", null,
                new BigDecimal("120000"), LocalDate.of(2027, 3, 1), null));
        User admin = user("postgres-price-admin", "ADMIN");
        String authorization = bearer(admin);

        mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", draft.id())
                        .header("Authorization", authorization).contentType("application/json")
                        .content("{\"rowVersion\":0,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.rowVersion").value(1));

        String revisionResponse = mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", draft.id())
                        .header("Authorization", authorization).contentType("application/json")
                        .content("{\"rowVersion\":1,\"amount\":130000}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.rowVersion").value(0))
                .andReturn().getResponse().getContentAsString();
        Number successorIdValue = com.jayway.jsonpath.JsonPath.read(revisionResponse, "$.data.id");
        Long successorId = successorIdValue.longValue();

        assertTrue(!draft.id().equals(successorId));
        assertEquals("INACTIVE", seatPriceRepository.findById(draft.id()).orElseThrow().getStatus());
        assertEquals(2L, seatPriceRepository.findById(draft.id()).orElseThrow().getRowVersion());
        assertEquals("ACTIVE", seatPriceRepository.findById(successorId).orElseThrow().getStatus());
        assertEquals(0L, seatPriceRepository.findById(successorId).orElseThrow().getRowVersion());
        assertEquals(4, auditLogRepository.count());
    }

    @Test
    void concurrentPatchRequestsAllowOneUpdateAndRejectTheStaleRevision() throws Exception {
        var draft = seatPriceAdminService.createDraft(new SeatPriceRequest("STANDARD", null,
                new BigDecimal("100000"), LocalDate.of(2027, 4, 1), null));
        String authorization = bearer(user("postgres-price-admin", "ADMIN"));
        CyclicBarrier startTogether = new CyclicBarrier(2);
        Callable<Integer> activate = () -> {
            startTogether.await();
            return mockMvc.perform(patch("/api/v1/admin/seat-prices/{id}", draft.id())
                            .header("Authorization", authorization).contentType("application/json")
                            .content("{\"rowVersion\":0,\"status\":\"ACTIVE\"}"))
                    .andReturn().getResponse().getStatus();
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(activate);
            Future<Integer> second = executor.submit(activate);
            int firstStatus = first.get();
            int secondStatus = second.get();

            assertTrue((firstStatus == 200 && secondStatus == 409)
                    || (firstStatus == 409 && secondStatus == 200));
        } finally {
            executor.shutdownNow();
        }

        SeatPrice updated = seatPriceRepository.findById(draft.id()).orElseThrow();
        assertEquals("ACTIVE", updated.getStatus());
        assertEquals(1L, updated.getRowVersion());
        assertEquals(2, auditLogRepository.count());
    }

    private User user(String username, String roleCode) {
        Role role = roleRepository.findByCode(roleCode).orElseThrow();
        return userRepository.findByUsername(username).orElseGet(() -> {
            User created = new User(); created.setUsername(username); created.setPasswordHash("hash");
            created.setFullName(username); created.setStatus(UserStatus.ACTIVE); created.setRole(role);
            return userRepository.saveAndFlush(created);
        });
    }

    private String bearer(User user) { return "Bearer " + jwtService.generateAccessToken(user); }
}
