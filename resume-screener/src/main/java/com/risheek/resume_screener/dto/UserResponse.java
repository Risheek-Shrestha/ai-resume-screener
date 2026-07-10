package com.risheek.resume_screener.dto;

import com.risheek.resume_screener.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String username;
    private String email;
    private User.Role role;

    private String phoneNumber;
    private LocalDate dateOfBirth;
    private String currentCollege;
    private String currentCourse;
    private Integer currentSemester;

    public static UserResponse from(User user) {

        String courseName = null;

        if (user.getCurrentCourse() != null) {
            courseName = user.getCurrentCourse().getName();
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .dateOfBirth(user.getDateOfBirth())
                .currentCollege(user.getCurrentCollege())
                .currentCourse(courseName)
                .currentSemester(user.getCurrentSemester())
                .build();
    }
}