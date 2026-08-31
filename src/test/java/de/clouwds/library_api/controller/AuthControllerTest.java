package de.clouwds.library_api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.clouwds.library_api.config.SecurityConfig;
import de.clouwds.library_api.dto.LoginRequest;
import de.clouwds.library_api.dto.MemberRequest;
import de.clouwds.library_api.dto.MemberResponse;
import de.clouwds.library_api.dto.RefreshRequest;
import de.clouwds.library_api.dto.TokenResponse;
import de.clouwds.library_api.model.Role;
import de.clouwds.library_api.repository.MemberRepository;
import de.clouwds.library_api.service.AuthService;
import de.clouwds.library_api.service.JwtService;
import de.clouwds.library_api.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private MemberRepository memberRepository;

    // Each test uses its own fake client IP so RateLimitFilter's per-IP bucket
    // (5 requests/minute on /auth/**) doesn't spill across independent tests.
    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    @Test
    void register_succeeds() throws Exception {
        MemberRequest request = new MemberRequest("Jane", "Doe", "jane@test.com", "password123", Role.MEMBER);
        when(memberService.createMember(org.mockito.ArgumentMatchers.any())).thenReturn(new MemberResponse(1L, "Jane", "Doe", "jane@test.com", Role.MEMBER));

        mockMvc.perform(post("/auth/register")
                        .with(fromIp("10.0.0.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/auth/register/1")));
    }

    @Test
    void login_validCredentials_succeeds() throws Exception {
        when(authService.login("jane@test.com", "password123"))
                .thenReturn(new TokenResponse("access-token", "Bearer", "refresh-token"));

        mockMvc.perform(post("/auth/login")
                        .with(fromIp("10.0.0.2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("jane@test.com", "password123"))))
                .andExpect(status().isOk());
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/login")
                        .with(fromIp("10.0.0.3"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("jane@test.com", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_succeeds() throws Exception {
        when(authService.refresh("valid-refresh-token"))
                .thenReturn(new TokenResponse("new-access-token", "Bearer", "new-refresh-token"));

        mockMvc.perform(post("/auth/refresh")
                        .with(fromIp("10.0.0.4"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("valid-refresh-token"))))
                .andExpect(status().isOk());
    }

    @Test
    void logout_succeeds() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .with(fromIp("10.0.0.5"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("valid-refresh-token"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void login_sixthRequestFromSameIp_isRateLimited() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new BadCredentialsException("Bad credentials"));
        String body = objectMapper.writeValueAsString(new LoginRequest("jane@test.com", "wrong"));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                    .with(fromIp("10.0.0.6"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body));
        }

        mockMvc.perform(post("/auth/login")
                        .with(fromIp("10.0.0.6"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());
    }
}
