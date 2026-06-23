package com.risheek.resume_screener.service;

import com.risheek.resume_screener.dto.RefreshRequest;
import com.risheek.resume_screener.entity.RefreshToken;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.exception.UserNotFoundException;
import com.risheek.resume_screener.repository.RefreshTokenRepository;
import com.risheek.resume_screener.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    private RefreshTokenService refreshTokenService;
    private WebClient webClient;

    @BeforeEach
    void setUp() {
        webClient = mock(WebClient.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));

        refreshTokenService = new RefreshTokenService(
                userRepository, refreshTokenRepository
        );

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication())
                .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", null));
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRefreshToken_happyPath_deletesOldTokenAndGeneratesNewOneWithExpiry() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken r = invocation.getArgument(0);
            r.setId(10);
            return r;
        });

        RefreshToken result = refreshTokenService.createRefreshToken("test@example.com");

        verify(refreshTokenRepository).deleteByUser(user);

        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getToken()).isNotBlank();
        Instant expectedExpiry = Instant.now().plusSeconds(7 * 24 * 60 * 60);
        assertThat(result.getExpiryDate()).isCloseTo(expectedExpiry, within(5, ChronoUnit.SECONDS));

    }

    @Test
    void createRefreshToken_userNotFound_throwsUsernameNotFoundException() {

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> refreshTokenService.createRefreshToken("test@example.com"));

        verify(refreshTokenRepository, never()).save(any());

    }

    @Test
    void verifyExpiration_notExpired_returnsSameToken(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken("qwertyuiop");
        refreshToken.setId(2);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));

        RefreshToken result = refreshTokenService.verifyExpiration(refreshToken);

        assertThat(result).isSameAs(refreshToken);

        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyExpiration_expired_deletesTokenAndThrowsRuntimeException(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken("qwertyuiop");
        refreshToken.setId(2);
        refreshToken.setExpiryDate(Instant.now().minusSeconds(3600));

        assertThrows(RuntimeException.class,
                () -> refreshTokenService.verifyExpiration(refreshToken));

        verify(refreshTokenRepository).delete(any());
    }

    @Test
    void findByToken_found_returnsOptionalWithToken(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken("qwertyuiop");
        refreshToken.setId(2);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("qwertyuiop")).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.findByToken("qwertyuiop");

        assertThat(result).isPresent();
        assertThat(result.get().getToken()).isEqualTo("qwertyuiop");
        assertThat(result.get().getId()).isEqualTo(2);
    }

    @Test
    void findByToken_notFound_returnsEmptyOptional(){

        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken("qwertyuiop");
        refreshToken.setId(2);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("qwertyuiop")).thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenService.findByToken("qwertyuiop");

        assertThat(result).isEmpty();

    }

}