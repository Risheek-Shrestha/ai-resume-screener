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
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       CourseRepository courseRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getCurrentUser() {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        return UserResponse.from(user);
    }

    public UserResponse updateCurrentUser(@Valid UpdateUserRequest request) {

        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        Course course = courseRepository.findById(request.getCurrentCourseId())
                .orElseThrow(() ->
                        new CourseNotFoundException(
                                "Course not found with id: " + request.getCurrentCourseId()));

        user.setUsername(request.getUsername());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setCurrentCollege(request.getCurrentCollege());
        user.setCurrentCourse(course);
        user.setCurrentSemester(request.getCurrentSemester());

        userRepository.save(user);

        return UserResponse.from(user);
    }

    public UserResponse createUser(@Valid RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        Course course = courseRepository.findById(request.getCurrentCourseId())
                .orElseThrow(() -> new CourseNotFoundException(
                        "Course not found with id: " + request.getCurrentCourseId()));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);

        user.setPhoneNumber(request.getPhoneNumber());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setCurrentCollege(request.getCurrentCollege());
        user.setCurrentCourse(course);
        user.setCurrentSemester(request.getCurrentSemester());

        userRepository.save(user);

        return UserResponse.from(user);
    }
}