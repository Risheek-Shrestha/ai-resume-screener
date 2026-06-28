package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.AuthRequest;
import com.risheek.resume_screener.dto.AuthResponse;
import com.risheek.resume_screener.dto.RefreshRequest;
import com.risheek.resume_screener.entity.RefreshToken;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.InvalidCredentialsException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.repository.RefreshTokenRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody AuthRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.USER);
        userRepository.save(user);
        return ResponseEntity.status(201).body("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid email or password");
        }

        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtUtil.generateToken(user.getEmail());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60)); // 7 days
        refreshTokenRepository.save(refreshToken);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken(), user.getEmail(), user.getUsername(), user.getRole().name()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElse(null);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(  refreshToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token expired, please login again");
        }

        String newAccessToken = jwtUtil.generateToken(refreshToken.getUser().getEmail());
        return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken.getToken(), refreshToken.getUser().getEmail(), refreshToken.getUser().getUsername(), refreshToken.getUser().getRole().name()));
    }

    @PostMapping("/revoke")
    public ResponseEntity<String> revoke(@RequestBody RefreshRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);
        return ResponseEntity.ok("Logged out successfully");
    }
}