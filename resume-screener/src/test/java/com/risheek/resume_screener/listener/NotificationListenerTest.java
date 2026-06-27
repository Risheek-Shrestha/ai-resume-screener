package com.risheek.resume_screener.listener;

import com.risheek.resume_screener.dto.ApplicationNotificationEvent;
import com.risheek.resume_screener.entity.ApplicationStatus;
import com.risheek.resume_screener.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private MailService mailService;

    private NotificationListener notificationListener;

    @BeforeEach
    void setUp() {
        notificationListener = new NotificationListener(mailService);
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
}
