package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.UserResponse;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    public UserResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        return UserResponse.from(user);
    }
}
