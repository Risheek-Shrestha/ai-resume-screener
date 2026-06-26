package com.risheek.resume_screener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.JobPageResponse;
import com.risheek.resume_screener.dto.JobRequest;
import com.risheek.resume_screener.dto.JobResponse;
import com.risheek.resume_screener.entity.Job;
import com.risheek.resume_screener.entity.ApplicationWindowStatus;
import com.risheek.resume_screener.exception.JobNotFoundException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.JobService;
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

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobController.class)
@Import({SecurityConfig.class, JobControllerTest.CacheTestConfig.class})
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;


    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createJob_validRequest() throws Exception {
        JobRequest request = new JobRequest();
        request.setTitle("Java Developer");
        request.setDescription("Spring Boot Developer");
        request.setSkills(List.of("Java", "Spring Boot"));
        request.setJobType(Job.JobType.FULL_TIME);
        request.setExperienceLevel(Job.ExperienceLevel.SENIOR);
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0));

        JobResponse response = new JobResponse();
        response.setId(1L);
        response.setTitle("Java Developer");
        response.setDescription("Spring Boot Developer");

        when(jobService.createJob(any(JobRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Java Developer"));

        verify(jobService).createJob(any(JobRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createJob_invalidRequest() throws Exception {

        JobRequest request = new JobRequest();
        request.setTitle("Java Developer");
        request.setDescription("Spring Boot Developer");
        request.setJobType(Job.JobType.FULL_TIME);
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0));

        JobResponse response = new JobResponse();
        response.setId(1L);
        response.setTitle("Java Developer");
        response.setDescription("Spring Boot Developer");
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 29, 0, 0, 0));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0));

        when(jobService.createJob(any(JobRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/jobs")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(jobService, never()).createJob(any());
    }

    @Test
    void getJobById_existingId_returns200() throws Exception {
        JobResponse response = new JobResponse(
                1L, "Backend Engineer", "Spring Boot role",
                null, null, List.of("Java", "Spring"), LocalDateTime.now(),
                LocalDateTime.of(2026, 6, 29, 0, 0, 0),
                LocalDateTime.of(2026, 7, 1, 17, 0, 0),
                ApplicationWindowStatus.OPEN);

        when(jobService.getJobById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Backend Engineer"));
    }

    @Test
    void getJobById_nonExistingId() throws Exception {

        JobResponse response = new JobResponse(
                1L, "Backend Engineer", "Spring Boot role",
                null, null, List.of("Java", "Spring"), LocalDateTime.now(),
                LocalDateTime.of(2026, 6, 29, 0, 0, 0),
                LocalDateTime.of(2026, 7, 1, 17, 0, 0),
                ApplicationWindowStatus.OPEN);

        when(jobService.getJobById(1L))
                .thenThrow(new JobNotFoundException("Job not found"));

        mockMvc.perform(get("/api/v1/jobs/1"))
                .andExpect(status().isNotFound());

    }

    @Test
    void getAllJobs_defaultPagination() throws Exception {

        JobResponse job1 = new JobResponse(
                1L, "Backend Engineer", "Spring Boot role",
                null, null, List.of("Java", "Spring"), LocalDateTime.now(),
                LocalDateTime.of(2026, 6, 29, 0, 0, 0),
                LocalDateTime.of(2026, 7, 1, 17, 0, 0),ApplicationWindowStatus.OPEN);


        JobResponse job2 = new JobResponse(
                2L,
                "Java Intern",
                "Internship role",
                Job.JobType.INTERNSHIP,
                Job.ExperienceLevel.JUNIOR,
                List.of("Java"),
                LocalDateTime.now(),
                LocalDateTime.of(2026, 6, 29, 0, 0, 0),
                LocalDateTime.of(2026, 7, 1, 17, 0, 0),
                ApplicationWindowStatus.OPEN
        );

        JobPageResponse response = new JobPageResponse(
                List.of(job1, job2), 0, 10, 2L, 1, true);

        when(jobService.getAllJobs(0, 10))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].title").value("Java Intern"));

        verify(jobService).getAllJobs(0, 10);
    }

    @Test
    void getAllJobs_customPagination_passesCorrectPageAndSize() throws Exception {

        JobResponse job = new JobResponse(
                1L,
                "Backend Engineer",
                "Spring Boot role",
                null,
                null,
                List.of("Java"),
                LocalDateTime.now(),
                LocalDateTime.of(2026, 6, 29, 0, 0, 0),
                LocalDateTime.of(2026, 7, 1, 17, 0, 0),
                ApplicationWindowStatus.OPEN
        );

        JobPageResponse response = new JobPageResponse(
                List.of(job), 2, 5, 1L, 1, true);

        when(jobService.getAllJobs(2, 5))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/jobs")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));

        verify(jobService).getAllJobs(2, 5);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateJob_validRequest_returns200() throws Exception {

        JobRequest request = new JobRequest();
        request.setTitle("Updated Backend Engineer");
        request.setDescription("Updated Spring Boot role");
        request.setSkills(List.of("Java", "Spring"));
        request.setJobType(Job.JobType.FULL_TIME);
        request.setExperienceLevel(Job.ExperienceLevel.SENIOR);
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0));

        JobResponse response = new JobResponse(
                1L,
                "Updated Backend Engineer",
                "Updated Spring Boot role",
                Job.JobType.FULL_TIME,
                Job.ExperienceLevel.SENIOR,
                List.of("Java", "Spring"),
                LocalDateTime.now(),
                LocalDateTime.of(2026, 6, 29, 0, 0, 0),
                LocalDateTime.of(2026, 7, 1, 17, 0, 0),
                ApplicationWindowStatus.OPEN
        );

        when(jobService.updateJob(1L, request))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/jobs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title")
                        .value("Updated Backend Engineer"));

        verify(jobService).updateJob(1L, request);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateJob_nonExistingId_returns404() throws Exception {

        JobRequest request = new JobRequest();
        request.setTitle("Backend Engineer");
        request.setDescription("Spring Boot role");
        request.setJobType(Job.JobType.FULL_TIME);
        request.setExperienceLevel(Job.ExperienceLevel.SENIOR);
        request.setSkills(List.of("Java"));
        request.setApplicationStartsAt(LocalDateTime.of(2026, 6, 26, 0, 0, 0));
        request.setApplicationDeadline(LocalDateTime.of(2026, 7, 1, 17, 0, 0));

        when(jobService.updateJob(eq(999L), any(JobRequest.class)))
                .thenThrow(new JobNotFoundException("Job not found"));

        mockMvc.perform(put("/api/v1/jobs/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateJob_invalidRequest_returns400() throws Exception {

        JobRequest request = new JobRequest();
        request.setTitle(null);

        mockMvc.perform(put("/api/v1/jobs/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(jobService, never())
                .updateJob(anyLong(), any(JobRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteJob_existingId_returns204() throws Exception {

        doNothing().when(jobService).deleteJob(1L);

        mockMvc.perform(delete("/api/v1/jobs/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(jobService).deleteJob(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteJob_nonExistingId_returns404() throws Exception {

        doThrow(new JobNotFoundException("Job not found"))
                .when(jobService)
                .deleteJob(999L);

        mockMvc.perform(delete("/api/v1/jobs/999")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(jobService).deleteJob(999L);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getOpenJobs_defaultPagination() throws Exception {

        JobResponse job = new JobResponse(
                1L,
                "Backend Engineer",
                "Spring Boot role",
                Job.JobType.FULL_TIME,
                Job.ExperienceLevel.MID,
                List.of("Java", "Spring"),
                LocalDateTime.now(),
                LocalDateTime.of(2026, 6, 26, 0, 0),
                LocalDateTime.of(2026, 7, 1, 17, 0),
                ApplicationWindowStatus.OPEN
        );

        JobPageResponse response = new JobPageResponse(
                List.of(job),
                0,
                10,
                1L,
                1,
                true
        );

        when(jobService.getOpenJobsForUser(0, 10))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/jobs/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Backend Engineer"));

        verify(jobService).getOpenJobsForUser(0, 10);
    }

    @Test
    @WithMockUser(roles = "USER")
    void getOpenJobs_customPagination() throws Exception {

        JobResponse job = new JobResponse(
                2L,
                "Java Developer",
                "Spring Boot",
                Job.JobType.FULL_TIME,
                Job.ExperienceLevel.SENIOR,
                List.of("Java"),
                LocalDateTime.now(),
                LocalDateTime.of(2026, 6, 26, 0, 0),
                LocalDateTime.of(2026, 7, 1, 17, 0),
                ApplicationWindowStatus.OPEN
        );

        JobPageResponse response = new JobPageResponse(
                List.of(job),
                2,
                5,
                1L,
                1,
                true
        );

        when(jobService.getOpenJobsForUser(2, 5))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/jobs/open")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Java Developer"));

        verify(jobService).getOpenJobsForUser(2, 5);
    }

    @TestConfiguration
    static class CacheTestConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("jobs");
        }
    }
}