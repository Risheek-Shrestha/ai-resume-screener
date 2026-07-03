package com.risheek.resume_screener.listener;

import com.risheek.resume_screener.dto.ApplicationNotificationEvent;
import com.risheek.resume_screener.entity.NotificationType;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.repository.UserRepository;
import com.risheek.resume_screener.service.MailService;
import com.risheek.resume_screener.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private final MailService mailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationListener(MailService mailService, NotificationService notificationService,
                                 UserRepository userRepository) {
        this.mailService = mailService;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = "applicationNotifications")
    public void handleNotification(ApplicationNotificationEvent event) {
        mailService.sendApplicationStatusEmail(event);

        userRepository.findByEmail(event.getEmail()).ifPresent(user ->
                notificationService.notifyUser(
                        user,
                        NotificationType.APPLICATION_STATUS_CHANGED,
                        statusTitle(event, user),
                        statusMessage(event),
                        null,
                        null
                )
        );
    }

    private String statusTitle(ApplicationNotificationEvent event, User user) {
        return switch (event.getStatus()) {
            case APPLIED -> "Application received: " + event.getJobTitle();
            case SHORTLISTED -> "You've been shortlisted: " + event.getJobTitle();
            case HIRED -> "Congratulations! Offer for " + event.getJobTitle();
            case REJECTED -> "Update on your application: " + event.getJobTitle();
        };
    }

    private String statusMessage(ApplicationNotificationEvent event) {
        return switch (event.getStatus()) {
            case APPLIED -> "Thanks for applying to " + event.getJobTitle() + ". We'll review your application and follow up soon.";
            case SHORTLISTED -> "Good news — you've been shortlisted for " + event.getJobTitle() + ". We'll be in touch about next steps.";
            case HIRED -> "Congratulations! You've been selected for " + event.getJobTitle() + ".";
            case REJECTED -> "Thank you for applying to " + event.getJobTitle() + ". We've decided to move forward with other candidates this time.";
        };
    }
}