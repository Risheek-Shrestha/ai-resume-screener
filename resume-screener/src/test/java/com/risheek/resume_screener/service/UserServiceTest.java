package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.RegisterRequest;
import com.risheek.resume_screener.dto.UpdateUserRequest;
import com.risheek.resume_screener.dto.UserResponse;
import com.risheek.resume_screener.entity.Course;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.CourseNotFoundException;
import com.risheek.resume_screener.exception.EmailAlreadyExistsException;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.CourseRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

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

    // ----- updateCurrentUser -----

    @Test
    void updateCurrentUser_validRequest_updatesAndReturnsUser() {

        Course existingCourse = new Course();
        existingCourse.setId(1L);
        existingCourse.setName("MCA");

        Course newCourse = new Course();
        newCourse.setId(2L);
        newCourse.setName("BCA");

        User user = new User();
        user.setId(1L);
        user.setUsername("olduser");
        user.setEmail("test@example.com");
        user.setRole(User.Role.USER);
        user.setCurrentCourse(existingCourse);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setUsername("newuser");
        request.setPhoneNumber("9999999999");
        request.setDateOfBirth(LocalDate.of(2001, 5, 10));
        request.setCurrentCollege("New College");
        request.setCurrentCourseId(2L);
        request.setCurrentSemester(4);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@example.com", null)
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(courseRepository.findById(2L))
                .thenReturn(Optional.of(newCourse));

        UserResponse response = userService.updateCurrentUser(request);

        assertEquals("newuser", response.getUsername());
        assertEquals("9999999999", response.getPhoneNumber());
        assertEquals("New College", response.getCurrentCollege());
        assertEquals("BCA", response.getCurrentCourse());
        assertEquals(4, response.getCurrentSemester());

        verify(userRepository).save(user);
    }

    @Test
    void updateCurrentUser_userNotFound_throwsException() {

        UpdateUserRequest request = new UpdateUserRequest();
        request.setCurrentCourseId(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("missing@example.com", null)
        );

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateCurrentUser(request)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateCurrentUser_courseNotFound_throwsException() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setCurrentCourseId(999L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("test@example.com", null)
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));
        when(courseRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                CourseNotFoundException.class,
                () -> userService.updateCurrentUser(request)
        );

        verify(userRepository, never()).save(any());
    }

    // ----- createUser -----

    @Test
    void createUser_validRequest_createsAndReturnsUser() {

        Course course = new Course();
        course.setId(1L);
        course.setName("MCA");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setPhoneNumber("9876543210");
        request.setDateOfBirth(LocalDate.of(2002, 1, 1));
        request.setCurrentCollege("Shoolini University");
        request.setCurrentCourseId(1L);
        request.setCurrentSemester(2);

        when(userRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());
        when(courseRepository.findById(1L))
                .thenReturn(Optional.of(course));
        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-password");

        UserResponse response = userService.createUser(request);

        assertEquals("newuser", response.getUsername());
        assertEquals("new@example.com", response.getEmail());
        assertEquals("MCA", response.getCurrentCourse());

        verify(userRepository).save(argThat(u ->
                u.getRole() == User.Role.USER
                        && "hashed-password".equals(u.getPasswordHash())
        ));
    }

    @Test
    void createUser_emailAlreadyExists_throwsException() {

        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");

        User existingUser = new User();
        existingUser.setEmail("existing@example.com");

        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any());
        verify(courseRepository, never()).findById(any());
    }

    @Test
    void createUser_courseNotFound_throwsException() {

        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setCurrentCourseId(999L);

        when(userRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());
        when(courseRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                CourseNotFoundException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository, never()).save(any());
    }
}