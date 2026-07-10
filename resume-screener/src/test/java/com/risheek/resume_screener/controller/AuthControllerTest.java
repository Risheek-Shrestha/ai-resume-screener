package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.*;
import com.risheek.resume_screener.entity.PasswordResetToken;
import com.risheek.resume_screener.entity.RefreshToken;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.repository.PasswordResetTokenRepository;
import com.risheek.resume_screener.repository.RefreshTokenRepository;
import com.risheek.resume_screener.repository.UserRepository;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.MailService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JacksonAutoConfiguration.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private RefreshTokenRepository refreshTokenRepository;
    @MockitoBean private CustomUserDetailService customUserDetailService;
    @MockitoBean
    private PasswordResetTokenRepository passwordResetTokenRepository;



    private final ObjectMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @MockitoBean
    private MailService mailService;

    @Test
    void login_validCredentials_returnsSuccessWithToken() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com")).thenReturn("access-token");

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").exists());

        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_invalidPassword_returnsUnauthorizedWithPlainBody() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                // The controller returns plain text "Invalid email or password" for wrong password
                .andExpect(content().string("Invalid email or password"));

        verify(jwtUtil, never()).generateToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_userNotFound_returnsUnauthorized() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(jwtUtil, never()).generateToken(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_validToken_returnSuccess_WithNewAccessToken() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid-refresh-token");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-refresh-token");
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        when(jwtUtil.generateToken("test@example.com")).thenReturn("new-access-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("valid-refresh-token"));

        verify(jwtUtil).generateToken("test@example.com");
    }

    @Test
    void refresh_tokenNotFound_returnsUnauthorized() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("nonexistent-token");

        when(refreshTokenRepository.findByToken("nonexistent-token"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid refresh token"));
    }

    @Test
    void refresh_invalidToken_returnsUnauthorized() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("invalid-token");

        when(refreshTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid refresh token"));

        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void refresh_tokenExpired_returnsUnauthorized() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid-refresh-token");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-refresh-token");
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().minusSeconds(3600));

        when(refreshTokenRepository.findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Refresh token expired, please login again"));
    }

    @Test
    void revoke_existingToken_deletesToken() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("valid-refresh-token");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("valid-refresh-token");
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(refreshToken));

        mockMvc.perform(post("/api/v1/auth/revoke")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));

        verify(refreshTokenRepository).delete(refreshToken);
    }

    @Test
    void revoke_tokenNotFound_stillReturnsOk() throws Exception {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("invalid-token");

        when(refreshTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/revoke")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void forgotPassword_existingUser_returnsOk() throws Exception {

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        User user = new User();
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "If an account exists for that email, a reset link has been sent."
                ));

        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).save(any());
        verify(mailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
    }

    @Test
    void forgotPassword_userNotFound_stillReturnsOk() throws Exception {

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("missing@example.com");

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailService, never()).sendPasswordResetEmail(any(), any());
    }

    @Test
    void resetPassword_validToken_returnsOk() throws Exception {

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("newPassword123");

        User user = new User();
        user.setPasswordHash("old-password");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("reset-token");
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(1800));
        token.setUsed(false);

        when(passwordResetTokenRepository.findByToken("reset-token"))
                .thenReturn(Optional.of(token));

        when(passwordEncoder.encode("newPassword123"))
                .thenReturn("encoded-password");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset successfully"));

        verify(userRepository).save(user);
        verify(passwordResetTokenRepository).save(token);
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test
    void resetPassword_invalidToken_returnsUnauthorized() throws Exception {

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setNewPassword("password");

        when(passwordResetTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or already used reset link"));
    }

    @Test
    void resetPassword_usedToken_returnsUnauthorized() throws Exception {

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("used-token");
        request.setNewPassword("password");

        PasswordResetToken token = new PasswordResetToken();
        token.setUsed(true);

        when(passwordResetTokenRepository.findByToken("used-token"))
                .thenReturn(Optional.of(token));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid or already used reset link"));
    }

    @Test
    void resetPassword_expiredToken_returnsUnauthorized() throws Exception {

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("password");

        PasswordResetToken token = new PasswordResetToken();
        token.setUsed(false);
        token.setExpiryDate(Instant.now().minusSeconds(60));

        when(passwordResetTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(token));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Reset link expired, please request a new one"));

        verify(passwordResetTokenRepository).delete(token);
    }
}