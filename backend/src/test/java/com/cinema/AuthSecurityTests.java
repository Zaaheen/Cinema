package com.cinema;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cinema.entity.Role;
import com.cinema.entity.User;
import com.cinema.entity.UserStatus;
import com.cinema.repository.RoleRepository;
import com.cinema.repository.UserRepository;
import com.cinema.security.JwtService;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.web.FilterChainProxy;

@SpringBootTest
@ActiveProfiles("test")
class AuthSecurityTests {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void refreshRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isForbidden());
    }

    @Test
    void csrfTokenAllowsRefreshRequestToReachAuthentication() throws Exception {
        MockHttpServletResponse csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse();
        String csrfToken = JsonPath.read(csrfResponse.getContentAsString(), "$.data.token");
        Cookie csrfCookie = csrfResponse.getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Origin", "http://localhost:5173")
                        .cookie(new Cookie("XSRF-TOKEN", csrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfBootstrapTokenAllowsCrossOriginLogoutToReachController() throws Exception {
        MockHttpServletResponse csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn()
                .getResponse();
        String csrfToken = JsonPath.read(csrfResponse.getContentAsString(), "$.data.token");
        Cookie csrfCookie = csrfResponse.getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Origin", "http://localhost:5173")
                        .cookie(new Cookie("XSRF-TOKEN", csrfCookie.getValue()))
                        .header("X-XSRF-TOKEN", csrfToken))
                .andExpect(status().isOk());
    }

    @Test
    void memberProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCannotAccessMemberProfile() throws Exception {
        User employee = saveUser("security-employee", "EMPLOYEE");

        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(employee)))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanAccessOwnProfile() throws Exception {
        User member = saveUser("security-member", "MEMBER");

        mockMvc.perform(get("/api/v1/members/me")
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("security-member"));
    }

    @Test
    void memberIsDeniedStaffAndAdminPaths() throws Exception {
        User member = saveUser("security-member-boundary", "MEMBER");
        String authorization = "Bearer " + jwtService.generateAccessToken(member);

        mockMvc.perform(get("/api/v1/staff/members").header("Authorization", authorization))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/movies").header("Authorization", authorization))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeIsDeniedAdminPaths() throws Exception {
        User employee = saveUser("security-employee-boundary", "EMPLOYEE");

        mockMvc.perform(get("/api/v1/admin/movies")
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(employee)))
                .andExpect(status().isForbidden());
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
}
