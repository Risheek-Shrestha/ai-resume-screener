package com.risheek.resume_screener.listener;

import com.risheek.resume_screener.dto.ApplicationNotificationEvent;
import com.risheek.resume_screener.service.MailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    private final MailService mailService;

    public NotificationListener(MailService mailService) {
        this.mailService = mailService;
    }

    @RabbitListener(queues = "applicationNotifications")
    public void handleNotification(ApplicationNotificationEvent event) {
        mailService.sendApplicationStatusEmail(event);
    }
}