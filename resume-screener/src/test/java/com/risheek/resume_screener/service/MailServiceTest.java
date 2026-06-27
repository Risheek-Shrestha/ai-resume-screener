package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.ApplicationNotificationEvent;
import com.risheek.resume_screener.entity.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender);
    }

    private ApplicationNotificationEvent buildEvent(ApplicationStatus status) {
        ApplicationNotificationEvent event = new ApplicationNotificationEvent();
        event.setEmail("applicant@example.com");
        event.setJobTitle("Backend Developer");
        event.setStatus(status);
        return event;
    }

    @Test
    void sendApplicationStatusEmail_appliedStatus_sendsCorrectSubjectAndBody() {
        ApplicationNotificationEvent event = buildEvent(ApplicationStatus.APPLIED);

        mailService.sendApplicationStatusEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("applicant@example.com");
        assertThat(sent.getSubject()).contains("Application received");
        assertThat(sent.getSubject()).contains("Backend Developer");
        assertThat(sent.getText()).contains("Thanks for applying");
    }

    @Test
    void sendApplicationStatusEmail_shortlistedStatus_sendsCorrectSubjectAndBody() {
        ApplicationNotificationEvent event = buildEvent(ApplicationStatus.SHORTLISTED);

        mailService.sendApplicationStatusEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getSubject()).contains("shortlisted");
        assertThat(sent.getText()).contains("shortlisted");
    }

    @Test
    void sendApplicationStatusEmail_hiredStatus_sendsCorrectSubjectAndBody() {
        ApplicationNotificationEvent event = buildEvent(ApplicationStatus.HIRED);

        mailService.sendApplicationStatusEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getSubject()).containsIgnoringCase("congratulations");
        assertThat(sent.getText()).containsIgnoringCase("selected");
    }

    @Test
    void sendApplicationStatusEmail_rejectedStatus_sendsCorrectSubjectAndBody() {
        ApplicationNotificationEvent event = buildEvent(ApplicationStatus.REJECTED);

        mailService.sendApplicationStatusEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getSubject()).contains("Update on your application");
        assertThat(sent.getText()).contains("other candidates");
    }

    @Test
    void sendApplicationStatusEmail_setsRecipientCorrectly() {
        ApplicationNotificationEvent event = buildEvent(ApplicationStatus.APPLIED);
        event.setEmail("custom@mail.com");

        mailService.sendApplicationStatusEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertThat(captor.getValue().getTo()).containsExactly("custom@mail.com");
    }
}
