package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.RefreshToken;
import com.risheek.resume_screener.entity.User;
import com.risheek.resume_screener.util.RepositoryTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class RefreshTokenRepositoryTest extends RepositoryTestHelper {

    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldCreateRefreshToken() {

        User currentUser = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(currentUser);
        refreshToken.setToken("asdfghjklzxcvbnm456123");
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        entityManager.flush();
        entityManager.clear();

        var found = refreshTokenRepository.findByToken(savedRefreshToken.getToken());

        assertTrue(found.isPresent());
        assertEquals("asdfghjklzxcvbnm456123", found.get().getToken());
        assertEquals(currentUser.getId(), found.get().getUser().getId());
    }

    @Test
    void shouldDeleteByUser() {

        User currentUser = createUser(
                "Risheek",
                "risheekshrestha@gmail.com"
        );

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(currentUser);
        refreshToken.setToken("asdfghjklzxcvbnm456123");
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7 * 24 * 60 * 60));

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        entityManager.flush();
        entityManager.clear();

        User managedUser = userRepository.findById(currentUser.getId()).orElseThrow();
        refreshTokenRepository.deleteByUser(managedUser);

        entityManager.flush();
        entityManager.clear();

        var remaining = refreshTokenRepository.findByToken(savedRefreshToken.getToken());

        assertTrue(remaining.isEmpty());
    }

    @Test
    void shouldReturnEmpty_whenTokenDoesNotExist() {

        var found = refreshTokenRepository.findByToken("this-token-does-not-exist");

        assertTrue(found.isEmpty());
    }
}