package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.ApplicationNotificationEvent;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendApplicationStatusEmail(ApplicationNotificationEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(event.getEmail());
        message.setSubject(subjectFor(event));
        message.setText(bodyFor(event));
        mailSender.send(message);
    }

    private String subjectFor(ApplicationNotificationEvent event) {
        return switch (event.getStatus()) {
            case APPLIED -> "Application received: " + event.getJobTitle();
            case SHORTLISTED -> "You've been shortlisted: " + event.getJobTitle();
            case HIRED -> "Congratulations! Offer for " + event.getJobTitle();
            case REJECTED -> "Update on your application: " + event.getJobTitle();
        };
    }

    private String bodyFor(ApplicationNotificationEvent event) {
        return switch (event.getStatus()) {
            case APPLIED -> "Thanks for applying to " + event.getJobTitle() + ". We'll review your application and follow up soon.";
            case SHORTLISTED -> "Good news — you've been shortlisted for " + event.getJobTitle() + ". We'll be in touch about next steps.";
            case HIRED -> "Congratulations! You've been selected for " + event.getJobTitle() + ".";
            case REJECTED -> "Thank you for applying to " + event.getJobTitle() + ". We've decided to move forward with other candidates this time.";
        };
    }
}