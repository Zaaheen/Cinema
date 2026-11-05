package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Role;
import com.cinema.repository.RoleRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AuthAccountFlowTests {

    private static final String INITIAL_PASSWORD = "valid-initial-password";
    private static final String REPLACEMENT_PASSWORD = "valid-replacement-password";

    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private RoleRepository roleRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
        if (roleRepository.findByCode("MEMBER").isEmpty()) {
            Role member = new Role();
            member.setCode("MEMBER");
            member.setName("Member");
            roleRepository.save(member);
        }
    }

    @Test
    void memberCanRegisterLoginReadAndUpdateProfileChangePasswordThenLoginAgain() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String username = "member" + suffix.substring(0, 12);
        String originalEmail = username + "@example.test";
        String updatedEmail = "updated" + suffix.substring(0, 12) + "@example.test";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","fullName":"Test Member","email":"%s"}
                                """.formatted(username, INITIAL_PASSWORD, originalEmail)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.username").value(username));

        String accessToken = loginAndReadAccessToken(username, INITIAL_PASSWORD);
        mockMvc.perform(get("/api/v1/members/me").header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(originalEmail));

        mockMvc.perform(patch("/api/v1/members/me")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Updated Member","email":"%s","phone":"0123456789"}
                                """.formatted(updatedEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Updated Member"))
                .andExpect(jsonPath("$.data.email").value(updatedEmail));

        mockMvc.perform(post("/api/v1/members/me/change-password")
                        .header("Authorization", bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(INITIAL_PASSWORD, REPLACEMENT_PASSWORD)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, INITIAL_PASSWORD)))
                .andExpect(status().isUnauthorized());

        loginAndReadAccessToken(username, REPLACEMENT_PASSWORD);
    }

    @Test
    void concurrentRefreshWithOneCookieHasAtMostOneSuccess() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String username = "race" + suffix.substring(0, 12);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s","fullName":"Race Member"}
                                """.formatted(username, INITIAL_PASSWORD)))
                .andExpect(status().isCreated());

        MockHttpServletResponse loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, INITIAL_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        Cookie refreshCookie = loginResponse.getCookie("refresh_token");
        MockHttpServletResponse csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk()).andReturn().getResponse();
        String csrfToken = csrfResponse.getCookie("XSRF-TOKEN").getValue();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> refresh = () -> {
            ready.countDown();
            start.await();
            return mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new Cookie("refresh_token", refreshCookie.getValue()), new Cookie("XSRF-TOKEN", csrfToken))
                            .header("X-XSRF-TOKEN", csrfToken))
                    .andReturn().getResponse().getStatus();
        };
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(refresh);
            Future<Integer> second = executor.submit(refresh);
            ready.await();
            start.countDown();
            List<Integer> statuses = List.of(first.get(), second.get());
            org.junit.jupiter.api.Assertions.assertEquals(1, statuses.stream().filter(code -> code == 200).count());
            org.junit.jupiter.api.Assertions.assertEquals(1, statuses.stream().filter(code -> code == 401).count());
        } finally {
            executor.shutdownNow();
        }
    }

    private String loginAndReadAccessToken(String username, String password) throws Exception {
        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        return JsonPath.read(response.getContentAsString(), "$.data.accessToken");
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
