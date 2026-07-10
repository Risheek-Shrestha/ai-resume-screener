package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.AuthRequest;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.repository.UserRepository;
import com.risheek.resume_screener.service.CustomUserDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private UserRepository userRepository;
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private CustomUserDetailService customUserDetailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createAdmin_withNewEmail_returnsCreated() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("adminuser");
        request.setEmail("admin@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Admin created successfully"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void createAdmin_savesUserWithAdminRoleAndEncodedPassword() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("adminuser");
        request.setEmail("admin@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(userRepository).save(argThatSavedAdmin());
    }

    private static User argThatSavedAdmin() {
        return org.mockito.ArgumentMatchers.argThat(user ->
                user.getRole() == User.Role.ADMIN
                        && "hashed-password".equals(user.getPasswordHash())
                        && "admin@example.com".equals(user.getEmail())
                        && "adminuser".equals(user.getUsername())
        );
    }

    @Test
    void createAdmin_emailAlreadyExists_returnsBadRequest() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("adminuser");
        request.setEmail("admin@example.com");
        request.setPassword("password123");

        User existingUser = new User();
        existingUser.setEmail("admin@example.com");

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(existingUser));

        mockMvc.perform(post("/api/v1/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already exists"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void createAdmin_withoutCsrf_isForbidden() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setUsername("adminuser");
        request.setEmail("admin@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(userRepository, never()).save(any());
    }
}