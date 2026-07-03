package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.NotificationPageResponse;
import com.risheek.resume_screener.dto.NotificationResponse;
import com.risheek.resume_screener.entity.Notification;
import com.risheek.resume_screener.entity.NotificationType;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.NotificationNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.NotificationRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // Used for the APPLICATION_STATUS_CHANGED notification, which only goes
    // to the one candidate whose application changed.
    public void notifyUser(User user, NotificationType type, String title, String message,
                            Long jobId, Long applicationId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setJobId(jobId);
        notification.setApplicationId(applicationId);
        notificationRepository.save(notification);
    }

    // Used for JOB_POSTED / JOB_OPEN_FOR_APPLY, which go out to every
    // candidate (USER role). Fanned out as one row per recipient so
    // read/unread state stays per-user.
    public void notifyAllUsers(NotificationType type, String title, String message, Long jobId) {
        List<User> recipients = userRepository.findAllByRole(User.Role.USER);

        List<Notification> notifications = recipients.stream().map(user -> {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setType(type);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setJobId(jobId);
            return notification;
        }).toList();

        notificationRepository.saveAll(notifications);
    }

    public NotificationPageResponse getMyNotifications(int page, int size) {
        User currentUser = getCurrentUser();

        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                currentUser.getId(), PageRequest.of(page, size));

        List<NotificationResponse> content = notificationPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new NotificationPageResponse(
                content,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isLast()
        );
    }

    public long getUnreadCount() {
        User currentUser = getCurrentUser();
        return notificationRepository.countByUserIdAndReadFalse(currentUser.getId());
    }

    public void markAsRead(Long notificationId) {
        User currentUser = getCurrentUser();

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedAccessException(
                    "You are not allowed to modify this notification");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    public void markAllAsRead() {
        User currentUser = getCurrentUser();
        notificationRepository.markAllAsReadForUser(currentUser.getId());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getJobId(),
                notification.getApplicationId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
