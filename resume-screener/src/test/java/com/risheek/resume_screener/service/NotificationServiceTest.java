package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.NotificationPageResponse;
import com.risheek.resume_screener.entity.Notification;
import com.risheek.resume_screener.entity.NotificationType;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.NotificationNotFoundException;
import com.risheek.resume_screener.exception.UnauthorizedAccessException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.NotificationRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository);

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", null));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        return user;
    }

    @Test
    void notifyUser_savesNotificationWithCorrectFields() {

        User user = buildUser(1L, "user@example.com");

        notificationService.notifyUser(user, NotificationType.APPLICATION_STATUS_CHANGED,
                "Status Update", "Your application was accepted", 10L, 20L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getType()).isEqualTo(NotificationType.APPLICATION_STATUS_CHANGED);
        assertThat(saved.getTitle()).isEqualTo("Status Update");
        assertThat(saved.getJobId()).isEqualTo(10L);
        assertThat(saved.getApplicationId()).isEqualTo(20L);
    }

    @Test
    void notifyAllUsers_fansOutToAllUsersWithUserRole() {

        User user1 = buildUser(1L, "user1@example.com");
        User user2 = buildUser(2L, "user2@example.com");

        when(userRepository.findAllByRole(User.Role.USER)).thenReturn(List.of(user1, user2));

        notificationService.notifyAllUsers(NotificationType.JOB_POSTED, "New Job", "A new job was posted", 5L);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        List<Notification> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(Notification::getUser).containsExactly(user1, user2);
        assertThat(saved).allMatch(n -> n.getType() == NotificationType.JOB_POSTED);
        assertThat(saved).allMatch(n -> n.getJobId().equals(5L));
    }

    @Test
    void getMyNotifications_returnsMappedPageResponse() {

        User user = buildUser(1L, "test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUser(user);
        notification.setType(NotificationType.JOB_POSTED);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setRead(false);

        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(page);

        NotificationPageResponse response = notificationService.getMyNotifications(0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().getFirst().getTitle()).isEqualTo("Title");
    }

    @Test
    void getUnreadCount_returnsCountFromRepository() {

        User user = buildUser(1L, "test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(notificationRepository.countByUserIdAndReadFalse(1L)).thenReturn(3L);

        long count = notificationService.getUnreadCount();

        assertThat(count).isEqualTo(3L);
    }

    @Test
    void markAsRead_happyPath() {

        User user = buildUser(1L, "test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Notification notification = new Notification();
        notification.setId(5L);
        notification.setUser(user);
        notification.setRead(false);

        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(5L);

        assertThat(notification.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_alreadyRead_doesNotSaveAgain() {

        User user = buildUser(1L, "test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        Notification notification = new Notification();
        notification.setId(5L);
        notification.setUser(user);
        notification.setRead(true);

        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(5L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_notFound_throwsException() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(buildUser(1L, "test@example.com")));
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class, () -> notificationService.markAsRead(99L));
    }

    @Test
    void markAsRead_notOwner_throwsUnauthorized() {

        User owner = buildUser(2L, "owner@example.com");
        User requester = buildUser(1L, "test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(requester));

        Notification notification = new Notification();
        notification.setId(5L);
        notification.setUser(owner);
        notification.setRead(false);

        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        assertThrows(UnauthorizedAccessException.class, () -> notificationService.markAsRead(5L));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAllAsRead_delegatesToRepository() {

        User user = buildUser(1L, "test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        notificationService.markAllAsRead();

        verify(notificationRepository).markAllAsReadForUser(1L);
    }

    @Test
    void getUnreadCount_userNotFound_throwsException() {

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> notificationService.getUnreadCount());
    }
}