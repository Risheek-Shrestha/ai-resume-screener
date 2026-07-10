package com.risheek.resume_screener.controller;

import com.risheek.resume_screener.dto.*;
import com.risheek.resume_screener.entity.PasswordResetToken;
import com.risheek.resume_screener.entity.RefreshToken;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.InvalidCredentialsException;
import com.risheek.resume_screener.jwt.JwtUtil;
import com.risheek.resume_screener.repository.PasswordResetTokenRepository;
import com.risheek.resume_screener.repository.RefreshTokenRepository;
import com.risheek.resume_screener.repository.UserRepository;
import com.risheek.resume_screener.service.MailService;
import jakarta.validation.Valid;
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            MailService mailService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailService = mailService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }

        refreshTokenRepository.deleteByUser(user);

        String accessToken = jwtUtil.generateToken(user.getEmail());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));

        refreshTokenRepository.save(refreshToken);

        return ResponseEntity.ok(
                new AuthResponse(
                        accessToken,
                        refreshToken.getToken(),
                        user.getEmail(),
                        user.getUsername(),
                        user.getRole().name()
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest request) {

        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElse(null);

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid refresh token");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Refresh token expired, please login again");
        }

        String accessToken =
                jwtUtil.generateToken(refreshToken.getUser().getEmail());

        return ResponseEntity.ok(
                new AuthResponse(
                        accessToken,
                        refreshToken.getToken(),
                        refreshToken.getUser().getEmail(),
                        refreshToken.getUser().getUsername(),
                        refreshToken.getUser().getRole().name()
                )
        );
    }

    @PostMapping("/revoke")
    public ResponseEntity<String> revoke(@RequestBody RefreshRequest request) {

        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(refreshTokenRepository::delete);

        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {

            passwordResetTokenRepository.deleteByUser(user);

            PasswordResetToken token = new PasswordResetToken();
            token.setToken(UUID.randomUUID().toString());
            token.setUser(user);
            token.setExpiryDate(Instant.now().plusSeconds(1800));
            token.setUsed(false);

            passwordResetTokenRepository.save(token);

            mailService.sendPasswordResetEmail(user.getEmail(), token.getToken());
        });

        return ResponseEntity.ok(
                "If an account exists for that email, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        PasswordResetToken token = passwordResetTokenRepository
                .findByToken(request.getToken())
                .orElse(null);

        if (token == null || token.isUsed()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or already used reset link");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {

            passwordResetTokenRepository.delete(token);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Reset link expired, please request a new one");
        }

        User user = token.getUser();

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        refreshTokenRepository.deleteByUser(user);

        return ResponseEntity.ok("Password reset successfully");
    }
}