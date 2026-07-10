package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.Notification;
import com.risheek.resume_screener.entity.NotificationType;
import com.risheek.resume_screener.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class NotificationRepositoryTest {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User createUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash("password");
        user.setRole(User.Role.USER);
        return userRepository.save(user);
    }

    private Notification createNotification(User user, NotificationType type, boolean read) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setRead(read);
        return notificationRepository.save(notification);
    }

    @Test
    void shouldFindByUserIdOrderByCreatedAtDesc() {

        User user = createUser("Risheek", "risheek@example.com");

        createNotification(user, NotificationType.JOB_POSTED, false);
        createNotification(user, NotificationType.JOB_OPEN_FOR_APPLY, false);

        entityManager.flush();
        entityManager.clear();

        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void shouldCountUnreadNotifications() {

        User user = createUser("Risheek", "risheek@example.com");

        createNotification(user, NotificationType.JOB_POSTED, false);
        createNotification(user, NotificationType.JOB_OPEN_FOR_APPLY, false);
        createNotification(user, NotificationType.APPLICATION_STATUS_CHANGED, true);

        entityManager.flush();
        entityManager.clear();

        long count = notificationRepository.countByUserIdAndReadFalse(user.getId());

        assertEquals(2, count);
    }

    @Test
    void shouldMarkAllAsReadForUser() {

        User user = createUser("Risheek", "risheek@example.com");

        createNotification(user, NotificationType.JOB_POSTED, false);
        createNotification(user, NotificationType.JOB_OPEN_FOR_APPLY, false);

        entityManager.flush();
        entityManager.clear();

        int updated = notificationRepository.markAllAsReadForUser(user.getId());

        entityManager.flush();
        entityManager.clear();

        assertEquals(2, updated);

        long remainingUnread = notificationRepository.countByUserIdAndReadFalse(user.getId());
        assertEquals(0, remainingUnread);
    }

    @Test
    void shouldNotAffectOtherUsersNotificationsWhenMarkingAllAsRead() {

        User user1 = createUser("UserOne", "user1@example.com");
        User user2 = createUser("UserTwo", "user2@example.com");

        createNotification(user1, NotificationType.JOB_POSTED, false);
        createNotification(user2, NotificationType.JOB_POSTED, false);

        entityManager.flush();
        entityManager.clear();

        notificationRepository.markAllAsReadForUser(user1.getId());

        entityManager.flush();
        entityManager.clear();

        assertEquals(0, notificationRepository.countByUserIdAndReadFalse(user1.getId()));
        assertEquals(1, notificationRepository.countByUserIdAndReadFalse(user2.getId()));
    }
}