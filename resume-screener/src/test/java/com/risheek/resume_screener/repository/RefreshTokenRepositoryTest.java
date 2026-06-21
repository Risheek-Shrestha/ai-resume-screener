package com.risheek.resume_screener.repository;

import com.risheek.resume_screener.entity.RefreshToken;
import com.risheek.resume_screener.entity.User;
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
class RefreshTokenRepositoryTest {

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
    void shouldCreateRefreshToken(){

        User user = new User();

        user.setUsername("Risheek");
        user.setEmail("risheekshrestha@gmail.com");
        user.setPasswordHash("risheek@1234");
        user.setRole(User.Role.USER);

        User currentUser =  userRepository.save(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(currentUser);
        refreshToken.setToken("asdfghjklzxcvbnm456123");
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7*24*60*60));

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        entityManager.flush();
        entityManager.clear();

        var found = refreshTokenRepository.findByToken(savedRefreshToken.getToken());
        assertEquals("asdfghjklzxcvbnm456123", found.get().getToken());

    }

    @Test
    void shouldDeleteByUserId(){

        User user = new User();

        user.setUsername("Risheek");
        user.setEmail("risheekshrestha@gmail.com");
        user.setPasswordHash("risheek@1234");
        user.setRole(User.Role.USER);

        User currentUser =  userRepository.save(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(currentUser);
        refreshToken.setToken("asdfghjklzxcvbnm456123");
        refreshToken.setExpiryDate(Instant.now().plusSeconds(7*24*60*60));

        RefreshToken savedRefreshToken = refreshTokenRepository.save(refreshToken);

        entityManager.flush();
        entityManager.clear();

        refreshTokenRepository.deleteByUser(currentUser);

        var remaining = refreshTokenRepository.findByToken(savedRefreshToken.getToken());
        assertTrue(remaining.isEmpty());
    }

}