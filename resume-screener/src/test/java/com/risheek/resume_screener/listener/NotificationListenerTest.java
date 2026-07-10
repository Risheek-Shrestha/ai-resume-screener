package com.risheek.resume_screener.listener;

import com.risheek.resume_screener.dto.ApplicationNotificationEvent;
import com.risheek.resume_screener.entity.ApplicationStatus;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.repository.UserRepository;
import com.risheek.resume_screener.service.MailService;
import com.risheek.resume_screener.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private MailService mailService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private UserRepository userRepository;

    private NotificationListener notificationListener;

    @BeforeEach
    void setUp() {
        notificationListener = new NotificationListener(mailService, notificationService, userRepository);

        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        lenient().when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void handleNotification_applied_delegatesToMailService() {
        ApplicationNotificationEvent event = new ApplicationNotificationEvent(
                "user@example.com", "Backend Developer", ApplicationStatus.APPLIED);

        notificationListener.handleNotification(event);

        verify(mailService).sendApplicationStatusEmail(event);
    }

    @Test
    void handleNotification_shortlisted_delegatesToMailService() {
        ApplicationNotificationEvent event = new ApplicationNotificationEvent(
                "user@example.com", "Backend Developer", ApplicationStatus.SHORTLISTED);

        notificationListener.handleNotification(event);

        verify(mailService).sendApplicationStatusEmail(event);
    }

    @Test
    void handleNotification_hired_delegatesToMailService() {
        ApplicationNotificationEvent event = new ApplicationNotificationEvent(
                "user@example.com", "Backend Developer", ApplicationStatus.HIRED);

        notificationListener.handleNotification(event);

        verify(mailService).sendApplicationStatusEmail(event);
    }

    @Test
    void handleNotification_rejected_delegatesToMailService() {
        ApplicationNotificationEvent event = new ApplicationNotificationEvent(
                "user@example.com", "Backend Developer", ApplicationStatus.REJECTED);

        notificationListener.handleNotification(event);

        verify(mailService).sendApplicationStatusEmail(event);
    }

    @Test
    void handleNotification_userFound_createsInAppNotification() {
        ApplicationNotificationEvent event = new ApplicationNotificationEvent(
                "user@example.com", "Backend Developer", ApplicationStatus.APPLIED);

        notificationListener.handleNotification(event);

        verify(notificationService).notifyUser(
                any(User.class),
                eq(com.risheek.resume_screener.entity.NotificationType.APPLICATION_STATUS_CHANGED),
                anyString(),
                anyString(),
                any(),
                any()
        );
    }

    @Test
    void handleNotification_userNotFound_stillSendsEmailButSkipsInAppNotification() {
        ApplicationNotificationEvent event = new ApplicationNotificationEvent(
                "unknown@example.com", "Backend Developer", ApplicationStatus.APPLIED);

        lenient().when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        notificationListener.handleNotification(event);

        verify(mailService).sendApplicationStatusEmail(event);
        verify(notificationService, never()).notifyUser(
                any(), any(), anyString(), anyString(), any(), any());
    }
}