package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.UserResponse;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JacksonAutoConfiguration.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void getCurrentUser_returnsUser() throws Exception {

        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(User.Role.USER)
                .phoneNumber("9876543210")
                .dateOfBirth(LocalDate.of(2002,1,1))
                .currentCollege("Shoolini University")
                .currentCourse("MCA")
                .currentSemester(2)
                .build();

        when(userService.getCurrentUser())
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.currentCourse").value("MCA"))
                .andExpect(jsonPath("$.currentSemester").value(2));
    }

    @Test
    void getCurrentUser_withoutAuthentication_returnsForbidden() throws Exception {

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }
}