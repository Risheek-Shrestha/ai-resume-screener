    package com.risheek.resume_screener.controller;

    import com.risheek.resume_screener.dto.AuthRequest;
    import com.risheek.resume_screener.dto.AuthResponse;
    import com.risheek.resume_screener.dto.ForgotPasswordRequest;
    import com.risheek.resume_screener.dto.RefreshRequest;
    import com.risheek.resume_screener.dto.ResetPasswordRequest;
    import com.risheek.resume_screener.dto.RegisterRequest;
    import com.risheek.resume_screener.entity.Course;
    import com.risheek.resume_screener.exception.CourseNotFoundException;
    import com.risheek.resume_screener.repository.CourseRepository;
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
        private final CourseRepository courseRepository;

        public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, RefreshTokenRepository refreshTokenRepository, PasswordResetTokenRepository passwordResetTokenRepository, MailService mailService, CourseRepository courseRepository) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
            this.jwtUtil = jwtUtil;
            this.refreshTokenRepository = refreshTokenRepository;
            this.passwordResetTokenRepository = passwordResetTokenRepository;
            this.mailService = mailService;
            this.courseRepository = courseRepository;
        }

        @PostMapping("/register")
        public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {

            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.badRequest().body("Email already exists");
            }

            Course course = courseRepository.findById(request.getCurrentCourseId())
                    .orElseThrow(() ->
                            new CourseNotFoundException(
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

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("User registered successfully");
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

        @PostMapping("/forgot-password")
        public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
            userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
                passwordResetTokenRepository.deleteByUser(user);

                PasswordResetToken resetToken = new PasswordResetToken();
                resetToken.setToken(UUID.randomUUID().toString());
                resetToken.setUser(user);
                resetToken.setExpiryDate(Instant.now().plusSeconds(30 * 60));
                resetToken.setUsed(false);
                passwordResetTokenRepository.save(resetToken);

                mailService.sendPasswordResetEmail(user.getEmail(), resetToken.getToken());
            });

            return ResponseEntity.ok("If an account exists for that email, a reset link has been sent.");
        }

        @PostMapping("/reset-password")
        public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
            PasswordResetToken resetToken = passwordResetTokenRepository
                    .findByToken(request.getToken())
                    .orElse(null);

            if (resetToken == null || resetToken.isUsed()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or already used reset link");
            }

            if (resetToken.getExpiryDate().isBefore(Instant.now())) {
                passwordResetTokenRepository.delete(resetToken);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Reset link expired, please request a new one");
            }

            User user = resetToken.getUser();
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);

            refreshTokenRepository.deleteByUser(user);

            return ResponseEntity.ok("Password reset successfully");
        }
    }