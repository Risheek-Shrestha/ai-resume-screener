package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.CourseRequest;
import com.risheek.resume_screener.dto.CourseResponse;
import com.risheek.resume_screener.exception.CourseInUseException;
import com.risheek.resume_screener.exception.CourseNotFoundException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CourseService;
import com.risheek.resume_screener.service.CustomUserDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CourseController.class)
@Import({SecurityConfig.class, CourseControllerTest.CacheTestConfig.class})
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CourseService courseService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    void getAllCourses_returnsList() throws Exception {

        CourseResponse response = new CourseResponse(1L, "MCA", 2);

        when(courseService.getAllCourses()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("MCA"))
                .andExpect(jsonPath("$[0].totalYears").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCourse_validRequest_returns201() throws Exception {

        CourseRequest request = new CourseRequest(null, "B.Tech", 4);
        CourseResponse response = new CourseResponse(1L, "B.Tech", 4);

        when(courseService.createCourse(any(CourseRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/courses")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("B.Tech"));

        verify(courseService).createCourse(any(CourseRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCourse_validRequest_returns200() throws Exception {

        CourseRequest request = new CourseRequest(null, "Updated Name", 3);
        CourseResponse response = new CourseResponse(1L, "Updated Name", 3);

        when(courseService.updateCourse(eq(1L), any(CourseRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/courses/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCourse_notFound_returns404() throws Exception {

        CourseRequest request = new CourseRequest(null, "Doesn't Matter", 3);

        when(courseService.updateCourse(eq(99L), any(CourseRequest.class)))
                .thenThrow(new CourseNotFoundException("Course not found with id: 99"));

        mockMvc.perform(put("/api/v1/courses/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCourse_happyPath_returns204() throws Exception {

        doNothing().when(courseService).deleteCourse(1L);

        mockMvc.perform(delete("/api/v1/courses/1").with(csrf()))
                .andExpect(status().isNoContent());

        verify(courseService).deleteCourse(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteCourse_inUse_returnsConflictOrBadRequest() throws Exception {

        doThrow(new CourseInUseException("Cannot delete course: in use"))
                .when(courseService).deleteCourse(1L);

        mockMvc.perform(delete("/api/v1/courses/1").with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @TestConfiguration
    static class CacheTestConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("courses");
        }
    }
}