package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.config.SecurityConfig;
import com.risheek.resume_screener.dto.NotificationPageResponse;
import com.risheek.resume_screener.dto.NotificationResponse;
import com.risheek.resume_screener.entity.NotificationType;
import com.risheek.resume_screener.exception.NotificationNotFoundException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.service.CustomUserDetailService;
import com.risheek.resume_screener.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import(SecurityConfig.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomUserDetailService customUserDetailService;

    private NotificationResponse sampleNotification() {
        return new NotificationResponse(
                1L, NotificationType.JOB_POSTED, "New Job", "A new job was posted",
                10L, null, false, LocalDateTime.of(2026, 7, 1, 9, 0)
        );
    }

    @Test
    @WithMockUser
    void getMyNotifications_returnsPageOf200() throws Exception {
        NotificationPageResponse page = new NotificationPageResponse(
                List.of(sampleNotification()), 0, 10, 1L, 1, true
        );
        when(notificationService.getMyNotifications(0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("New Job"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(notificationService).getMyNotifications(0, 10);
    }

    @Test
    @WithMockUser
    void getMyNotifications_usesDefaultPagingParams() throws Exception {
        NotificationPageResponse page = new NotificationPageResponse(List.of(), 0, 10, 0L, 0, true);
        when(notificationService.getMyNotifications(0, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk());

        verify(notificationService).getMyNotifications(0, 10);
    }

    @Test
    @WithMockUser
    void getUnreadCount_returns200() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));

        verify(notificationService).getUnreadCount();
    }

    @Test
    @WithMockUser
    void markAsRead_existing_returns204() throws Exception {
        doNothing().when(notificationService).markAsRead(1L);

        mockMvc.perform(put("/api/v1/notifications/1/read")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(1L);
    }

    @Test
    @WithMockUser
    void markAsRead_nonExisting_returns404() throws Exception {
        doThrow(new NotificationNotFoundException("Notification not found"))
                .when(notificationService).markAsRead(999L);

        mockMvc.perform(put("/api/v1/notifications/999/read")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void markAllAsRead_returns204() throws Exception {
        doNothing().when(notificationService).markAllAsRead();

        mockMvc.perform(put("/api/v1/notifications/read-all")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead();
    }
}