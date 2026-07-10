package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.UserResponse;
import com.risheek.resume_screener.entity.Course;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getCurrentUser_returnsUserResponse() {

        Course course = new Course();
        course.setId(1L);
        course.setName("MCA");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setRole(User.Role.USER);
        user.setPhoneNumber("9876543210");
        user.setDateOfBirth(LocalDate.of(2002,1,1));
        user.setCurrentCollege("Shoolini University");
        user.setCurrentCourse(course);
        user.setCurrentSemester(2);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test@example.com",
                        null
                )
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser();

        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("9876543210", response.getPhoneNumber());
        assertEquals("MCA", response.getCurrentCourse());

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void getCurrentUser_userNotFound_throwsException() {

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "missing@example.com",
                        null
                )
        );

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getCurrentUser()
        );

        verify(userRepository).findByEmail("missing@example.com");
    }
}