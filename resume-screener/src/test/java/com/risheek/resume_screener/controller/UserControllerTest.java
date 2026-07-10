package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.RegisterRequest;
import com.risheek.resume_screener.dto.UpdateUserRequest;
import com.risheek.resume_screener.dto.UserResponse;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.CourseNotFoundException;
import com.risheek.resume_screener.exception.EmailAlreadyExistsException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    // ----- POST /register -----

    @Test
    void register_validRequest_returnsCreated() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("9876543210");
        request.setDateOfBirth(LocalDate.of(2002, 1, 1));
        request.setCurrentCollege("Shoolini University");
        request.setCurrentCourseId(1L);
        request.setCurrentSemester(2);

        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("newuser")
                .email("new@example.com")
                .role(User.Role.USER)
                .build();

        when(userService.createUser(any(RegisterRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void register_emailAlreadyExists_returnsConflict() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userService.createUser(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/api/v1/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidRequest_returnsBadRequest() throws Exception {

        RegisterRequest request = new RegisterRequest();
        // missing required fields (username, email, password)

        mockMvc.perform(post("/api/v1/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).createUser(any());
    }

    @Test
    void register_courseNotFound_returnsNotFound() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("9876543210");
        request.setDateOfBirth(LocalDate.of(2002, 1, 1));
        request.setCurrentCollege("Shoolini University");
        request.setCurrentCourseId(999L);
        request.setCurrentSemester(2);

        when(userService.createUser(any(RegisterRequest.class)))
                .thenThrow(new CourseNotFoundException("Course not found with id: 999"));

        mockMvc.perform(post("/api/v1/users/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ----- PUT /me -----

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void updateCurrentUser_validRequest_returnsUpdatedUser() throws Exception {

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("updateduser");
        request.setPhoneNumber("9999999999");
        request.setDateOfBirth(LocalDate.of(2001, 5, 10));
        request.setCurrentCollege("New College");
        request.setCurrentCourseId(2L);
        request.setCurrentSemester(4);

        UserResponse response = UserResponse.builder()
                .id(1L)
                .username("updateduser")
                .email("test@example.com")
                .role(User.Role.USER)
                .phoneNumber("9999999999")
                .currentCollege("New College")
                .currentCourse("BCA")
                .currentSemester(4)
                .build();

        when(userService.updateCurrentUser(any(UpdateUserRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("updateduser"))
                .andExpect(jsonPath("$.currentCourse").value("BCA"))
                .andExpect(jsonPath("$.currentSemester").value(4));
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void updateCurrentUser_courseNotFound_returnsNotFound() throws Exception {

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("updateduser");
        request.setPhoneNumber("9999999999");
        request.setDateOfBirth(LocalDate.of(2001, 5, 10));
        request.setCurrentCollege("New College");
        request.setCurrentCourseId(999L);
        request.setCurrentSemester(4);

        when(userService.updateCurrentUser(any(UpdateUserRequest.class)))
                .thenThrow(new CourseNotFoundException("Course not found with id: 999"));

        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test@example.com", roles = "USER")
    void updateCurrentUser_invalidRequest_returnsBadRequest() throws Exception {

        UpdateUserRequest request = new UpdateUserRequest();
        // missing required fields

        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateCurrentUser(any());
    }

    @Test
    void updateCurrentUser_withoutAuthentication_returnsForbidden() throws Exception {

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("updateduser");
        request.setPhoneNumber("9999999999");
        request.setDateOfBirth(LocalDate.of(2001, 5, 10));
        request.setCurrentCollege("New College");
        request.setCurrentCourseId(2L);
        request.setCurrentSemester(4);

        mockMvc.perform(put("/api/v1/users/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}